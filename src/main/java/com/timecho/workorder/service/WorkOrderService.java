package com.timecho.workorder.service;

import com.timecho.workorder.dto.AssignWorkOrderRequest;
import com.timecho.workorder.dto.AttachmentRequest;
import com.timecho.workorder.dto.BulkStatusUpdateRequest;
import com.timecho.workorder.dto.PortalCommentRequest;
import com.timecho.workorder.dto.RequirementEvaluationRequest;
import com.timecho.workorder.dto.WorkOrderCommentRequest;
import com.timecho.workorder.dto.WorkOrderRequest;
import com.timecho.workorder.dto.WorkOrderStatusRequest;
import com.timecho.workorder.model.AssignmentRule;
import com.timecho.workorder.model.Department;
import com.timecho.workorder.model.Priority;
import com.timecho.workorder.model.RequirementEvaluation;
import com.timecho.workorder.model.SlaPolicy;
import com.timecho.workorder.model.Status;
import com.timecho.workorder.model.User;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.model.WorkOrderAttachment;
import com.timecho.workorder.model.WorkOrderComment;
import com.timecho.workorder.model.WorkOrderHistory;
import com.timecho.workorder.model.WorkOrderType;
import com.timecho.workorder.repository.DepartmentRepository;
import com.timecho.workorder.repository.PriorityRepository;
import com.timecho.workorder.repository.RequirementEvaluationRepository;
import com.timecho.workorder.repository.StatusRepository;
import com.timecho.workorder.repository.UserRepository;
import com.timecho.workorder.repository.WorkOrderAttachmentRepository;
import com.timecho.workorder.repository.WorkOrderCommentRepository;
import com.timecho.workorder.repository.WorkOrderHistoryRepository;
import com.timecho.workorder.repository.WorkOrderNotificationRepository;
import com.timecho.workorder.repository.WorkOrderRepository;
import com.timecho.workorder.repository.WorkOrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = new HashMap<>();
    static {
        STATUS_TRANSITIONS.put("PENDING", new LinkedHashSet<>(Arrays.asList("IN_PROGRESS", "CANCELLED")));
        STATUS_TRANSITIONS.put("IN_PROGRESS", new LinkedHashSet<>(Arrays.asList("COMPLETED", "CANCELLED", "PENDING")));
        STATUS_TRANSITIONS.put("COMPLETED", new LinkedHashSet<>(Collections.singletonList("IN_PROGRESS")));
        STATUS_TRANSITIONS.put("CANCELLED", new LinkedHashSet<>(Arrays.asList("PENDING", "IN_PROGRESS")));
    }

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private WorkOrderHistoryRepository workOrderHistoryRepository;

    @Autowired
    private WorkOrderCommentRepository workOrderCommentRepository;

    @Autowired
    private WorkOrderAttachmentRepository workOrderAttachmentRepository;

    @Autowired
    private RequirementEvaluationRepository requirementEvaluationRepository;

    @Autowired
    private WorkOrderNotificationRepository workOrderNotificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PriorityRepository priorityRepository;

    @Autowired
    private WorkOrderTypeRepository workOrderTypeRepository;

    @Autowired
    private AssignmentRuleService assignmentRuleService;

    @Autowired
    private SlaPolicyService slaPolicyService;

    @Autowired
    private WorkOrderNotificationService notificationService;

    @Autowired
    private FeishuIntegrationService feishuIntegrationService;

    @Transactional
    public WorkOrder createWorkOrder(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        populateWorkOrder(workOrder, request);
        boolean autoAssigned = applyAutoAssignmentIfNeeded(workOrder);
        applySlaDeadlineIfMissing(workOrder);
        applyStatusLifecycle(workOrder, workOrder.getStatus() == null ? null : workOrder.getStatus().getName(), null);
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        User operator = resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester());
        recordHistory(savedWorkOrder, operator, "CREATE", "工单创建");
        if (autoAssigned) {
            recordHistory(savedWorkOrder, operator, "AUTO_ASSIGN", "根据分派规则自动指派给 " + savedWorkOrder.getAssignee().getName());
        }
        notificationService.createDualNotification(savedWorkOrder, "CREATE", "工单已创建：" + savedWorkOrder.getTitle());
        feishuIntegrationService.notifyWorkOrderEvent("CREATE", savedWorkOrder, "工单已创建");
        return savedWorkOrder;
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> getWorkOrderById(Long id) {
        return workOrderRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> getPortalWorkOrderById(Long id, String customerEmail) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        validateCustomerAccess(workOrder, customerEmail);
        return Optional.of(workOrder);
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> searchWorkOrders(String keyword,
                                            Long requesterId,
                                            Long assigneeId,
                                            Long departmentId,
                                            Long statusId,
                                            Long priorityId,
                                            Long typeId,
                                            String source,
                                            String customerType,
                                            String productName,
                                            String tag,
                                            Boolean overdue) {
        Specification<WorkOrder> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("customerEmail")), pattern)
                ));
            }

            if (requesterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("requester").get("id"), requesterId));
            }
            if (assigneeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (departmentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("department").get("id"), departmentId));
            }
            if (statusId != null) {
                predicates.add(criteriaBuilder.equal(root.get("status").get("id"), statusId));
            }
            if (priorityId != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority").get("id"), priorityId));
            }
            if (typeId != null) {
                predicates.add(criteriaBuilder.equal(root.get("type").get("id"), typeId));
            }
            if (StringUtils.hasText(source)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("source")), source.trim().toLowerCase()));
            }
            if (StringUtils.hasText(customerType)) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("customerType")), customerType.trim().toLowerCase()));
            }
            if (StringUtils.hasText(productName)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + productName.trim().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(tag)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), "%" + tag.trim().toLowerCase() + "%"));
            }
            if (Boolean.TRUE.equals(overdue)) {
                predicates.add(criteriaBuilder.isNotNull(root.get("dueAt")));
                predicates.add(criteriaBuilder.lessThan(root.<LocalDateTime>get("dueAt"), LocalDateTime.now()));
                predicates.add(criteriaBuilder.not(root.get("status").get("name").in("COMPLETED", "CANCELLED")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return workOrderRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<WorkOrder> searchPortalWorkOrders(String customerEmail, String keyword, Long statusId) {
        if (!StringUtils.hasText(customerEmail)) {
            throw new IllegalArgumentException("客户邮箱不能为空");
        }
        return workOrderRepository.findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(customerEmail.trim()).stream()
            .filter(workOrder -> !StringUtils.hasText(keyword)
                || containsIgnoreCase(workOrder.getTitle(), keyword)
                || containsIgnoreCase(workOrder.getDescription(), keyword)
                || containsIgnoreCase(workOrder.getProductName(), keyword))
            .filter(workOrder -> statusId == null || (workOrder.getStatus() != null && statusId.equals(workOrder.getStatus().getId())))
            .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrder updateWorkOrder(Long id, WorkOrderRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        String previousStatus = workOrder.getStatus() == null ? null : workOrder.getStatus().getName();
        populateWorkOrder(workOrder, request);
        validateStatusTransition(previousStatus, workOrder.getStatus() == null ? null : workOrder.getStatus().getName());
        boolean autoAssigned = applyAutoAssignmentIfNeeded(workOrder);
        applySlaDeadlineIfMissing(workOrder);
        applyStatusLifecycle(workOrder, workOrder.getStatus() == null ? null : workOrder.getStatus().getName(), previousStatus);
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        User operator = resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester());
        recordHistory(savedWorkOrder, operator, "UPDATE", "工单基础信息更新");
        if (autoAssigned) {
            recordHistory(savedWorkOrder, operator, "AUTO_ASSIGN", "根据分派规则自动指派给 " + savedWorkOrder.getAssignee().getName());
        }
        notificationService.createDualNotification(savedWorkOrder, "UPDATE", "工单信息已更新：" + savedWorkOrder.getTitle());
        feishuIntegrationService.notifyWorkOrderEvent("UPDATE", savedWorkOrder, "工单基础信息更新");
        return savedWorkOrder;
    }

    @Transactional
    @Retryable(
        value = {CannotAcquireLockException.class, PessimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120, multiplier = 2.0)
    )
    public WorkOrder assignWorkOrder(Long id, AssignWorkOrderRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        User assignee = getRequiredUser(request.getAssigneeId());
        workOrder.setAssignee(assignee);
        if (workOrder.getFirstResponseAt() == null) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }
        if (workOrder.getStatus() != null && "PENDING".equalsIgnoreCase(workOrder.getStatus().getName())) {
            statusRepository.findByName("IN_PROGRESS").ifPresent(workOrder::setStatus);
        }

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        String description = StringUtils.hasText(request.getRemark())
            ? "工单已指派给 " + assignee.getName() + "，备注：" + request.getRemark().trim()
            : "工单已指派给 " + assignee.getName();
        recordHistory(savedWorkOrder, resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester()), "ASSIGN", description);
        notificationService.createDualNotification(savedWorkOrder, "ASSIGN", description);
        feishuIntegrationService.notifyWorkOrderEvent("ASSIGN", savedWorkOrder, description);
        return savedWorkOrder;
    }

    @Transactional
    @Retryable(
        value = {CannotAcquireLockException.class, PessimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120, multiplier = 2.0)
    )
    public WorkOrder updateStatus(Long id, WorkOrderStatusRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        Status status = getRequiredStatus(request.getStatusId());
        String previousStatus = workOrder.getStatus() == null ? null : workOrder.getStatus().getName();
        validateStatusTransition(previousStatus, status.getName());
        workOrder.setStatus(status);
        applyStatusLifecycle(workOrder, status.getName(), previousStatus);
        if (workOrder.getFirstResponseAt() == null && !"PENDING".equalsIgnoreCase(status.getName())) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        String description = StringUtils.hasText(request.getRemark())
            ? "工单状态更新为 " + status.getDescription() + "，备注：" + request.getRemark().trim()
            : "工单状态更新为 " + status.getDescription();
        recordHistory(savedWorkOrder, resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester()), "STATUS_CHANGE", description);
        notificationService.createDualNotification(savedWorkOrder, "STATUS_CHANGE", description);
        feishuIntegrationService.notifyWorkOrderEvent("STATUS_CHANGE", savedWorkOrder, description);
        return savedWorkOrder;
    }

    @Transactional
    @Retryable(
        value = {CannotAcquireLockException.class, PessimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 120, multiplier = 2.0)
    )
    public List<WorkOrder> bulkUpdateStatus(BulkStatusUpdateRequest request) {
        Status status = getRequiredStatus(request.getStatusId());
        User explicitOperator = request.getOperatorId() == null ? null : getRequiredUser(request.getOperatorId());

        return request.getWorkOrderIds().stream()
            .map(workOrderId -> {
                WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
                String previousStatus = workOrder.getStatus() == null ? null : workOrder.getStatus().getName();
                validateStatusTransition(previousStatus, status.getName());
                workOrder.setStatus(status);
                applyStatusLifecycle(workOrder, status.getName(), previousStatus);
                WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
                User operator = explicitOperator != null ? explicitOperator : savedWorkOrder.getRequester();
                String description = StringUtils.hasText(request.getRemark())
                    ? "批量更新状态为 " + status.getDescription() + "，备注：" + request.getRemark().trim()
                    : "批量更新状态为 " + status.getDescription();
                recordHistory(savedWorkOrder, operator, "BULK_STATUS_CHANGE", description);
                notificationService.createDualNotification(savedWorkOrder, "BULK_STATUS_CHANGE", description);
                feishuIntegrationService.notifyWorkOrderEvent("BULK_STATUS_CHANGE", savedWorkOrder, description);
                return savedWorkOrder;
            })
            .collect(Collectors.toList());
    }

    @Transactional
    public WorkOrderComment addComment(Long workOrderId, WorkOrderCommentRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
        User user = getRequiredUser(request.getUserId());

        WorkOrderComment comment = new WorkOrderComment();
        comment.setWorkOrder(workOrder);
        comment.setUser(user);
        comment.setContent(request.getContent().trim());
        comment.setInternalOnly(request.isInternalOnly());
        WorkOrderComment savedComment = workOrderCommentRepository.save(comment);

        String visibility = request.isInternalOnly() ? "内部评论" : "公开评论";
        String description = visibility + "：" + abbreviate(request.getContent().trim());
        recordHistory(workOrder, user, "COMMENT", description);
        notificationService.createDualNotification(workOrder, "COMMENT", description);
        feishuIntegrationService.notifyWorkOrderEvent("COMMENT", workOrder, description);
        return savedComment;
    }

    @Transactional
    public WorkOrderComment addPortalComment(Long workOrderId, PortalCommentRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
        validateCustomerAccess(workOrder, request.getCustomerEmail());
        User portalBot = userRepository.findByUsername("portal_bot").orElse(workOrder.getRequester());

        String formattedContent = "[客户:" + request.getCustomerName().trim() + "<" + request.getCustomerEmail().trim() + ">] " + request.getContent().trim();
        WorkOrderComment comment = new WorkOrderComment();
        comment.setWorkOrder(workOrder);
        comment.setUser(portalBot);
        comment.setContent(formattedContent);
        comment.setInternalOnly(false);
        WorkOrderComment savedComment = workOrderCommentRepository.save(comment);

        String description = "客户评论：" + abbreviate(formattedContent);
        recordHistory(workOrder, portalBot, "CUSTOMER_COMMENT", description);
        notificationService.createDualNotification(workOrder, "CUSTOMER_COMMENT", description);
        feishuIntegrationService.notifyWorkOrderEvent("CUSTOMER_COMMENT", workOrder, description);
        return savedComment;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderComment> getComments(Long workOrderId) {
        getRequiredWorkOrder(workOrderId);
        return workOrderCommentRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderComment> getPortalComments(Long workOrderId, String customerEmail) {
        WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
        validateCustomerAccess(workOrder, customerEmail);
        return workOrderCommentRepository.findByWorkOrderIdAndInternalOnlyFalseOrderByCreatedAtAsc(workOrderId);
    }

    @Transactional
    public WorkOrderAttachment addAttachment(Long workOrderId, AttachmentRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
        User user = getRequiredUser(request.getUploadedById());

        WorkOrderAttachment attachment = new WorkOrderAttachment();
        attachment.setWorkOrder(workOrder);
        attachment.setUploadedBy(user);
        attachment.setFileName(request.getFileName().trim());
        attachment.setFileUrl(request.getFileUrl().trim());
        attachment.setContentType(request.getContentType());
        attachment.setSizeBytes(request.getSizeBytes());
        WorkOrderAttachment savedAttachment = workOrderAttachmentRepository.save(attachment);

        String message = "上传附件：" + savedAttachment.getFileName();
        recordHistory(workOrder, user, "ATTACHMENT", message);
        notificationService.createDualNotification(workOrder, "ATTACHMENT", message);
        feishuIntegrationService.notifyWorkOrderEvent("ATTACHMENT", workOrder, message);
        return savedAttachment;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderAttachment> getAttachments(Long workOrderId) {
        getRequiredWorkOrder(workOrderId);
        return workOrderAttachmentRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
    }

    @Transactional
    public RequirementEvaluation evaluateRequirement(Long workOrderId, RequirementEvaluationRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
        User evaluator = getRequiredUser(request.getEvaluatorId());

        RequirementEvaluation evaluation = new RequirementEvaluation();
        evaluation.setWorkOrder(workOrder);
        evaluation.setEvaluator(evaluator);
        evaluation.setRequirementValue(request.getRequirementValue());
        evaluation.setDevelopmentEffort(request.getDevelopmentEffort());
        evaluation.setCustomerWeight(request.getCustomerWeight());
        evaluation.setCompetitorImpact(request.getCompetitorImpact());
        evaluation.setImpactScopeScore(request.getImpactScopeScore());
        evaluation.setComment(request.getComment());
        int totalScore = calculateTotalScore(request);
        evaluation.setTotalScore(totalScore);
        RequirementEvaluation savedEvaluation = requirementEvaluationRepository.save(evaluation);

        priorityRepository.findByName(mapPriorityByScore(totalScore)).ifPresent(workOrder::setPriority);
        workOrderRepository.save(workOrder);
        String message = "需求评估完成，总分：" + totalScore;
        recordHistory(workOrder, evaluator, "EVALUATION", message);
        notificationService.createDualNotification(workOrder, "EVALUATION", message);
        feishuIntegrationService.notifyWorkOrderEvent("EVALUATION", workOrder, message);
        return savedEvaluation;
    }

    @Transactional(readOnly = true)
    public List<RequirementEvaluation> getEvaluations(Long workOrderId) {
        getRequiredWorkOrder(workOrderId);
        return requirementEvaluationRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
    }

    @Transactional
    public void deleteWorkOrder(Long id) {
        getRequiredWorkOrder(id);
        workOrderNotificationRepository.deleteByWorkOrderId(id);
        requirementEvaluationRepository.deleteByWorkOrderId(id);
        workOrderAttachmentRepository.deleteByWorkOrderId(id);
        workOrderCommentRepository.deleteByWorkOrderId(id);
        workOrderHistoryRepository.deleteByWorkOrderId(id);
        workOrderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderHistory> getWorkOrderHistory(Long workOrderId) {
        getRequiredWorkOrder(workOrderId);
        return workOrderHistoryRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatistics() {
        List<WorkOrder> workOrders = workOrderRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long total = workOrders.size();
        long completed = workOrders.stream().filter(workOrder -> hasStatus(workOrder, "COMPLETED")).count();
        long inProgress = workOrders.stream().filter(workOrder -> hasStatus(workOrder, "IN_PROGRESS")).count();
        long pending = workOrders.stream().filter(workOrder -> hasStatus(workOrder, "PENDING")).count();
        long overdue = workOrders.stream()
            .filter(workOrder -> workOrder.getDueAt() != null && workOrder.getDueAt().isBefore(now) && !isClosed(workOrder))
            .count();
        long responseTimeout = workOrders.stream()
            .filter(workOrder -> workOrder.getResponseDueAt() != null && workOrder.getFirstResponseAt() == null && workOrder.getResponseDueAt().isBefore(now))
            .count();
        long responseBreached = workOrders.stream().filter(WorkOrder::isResponseBreached).count();
        long resolutionBreached = workOrders.stream().filter(WorkOrder::isResolutionBreached).count();
        long escalated = workOrders.stream().filter(WorkOrder::isEscalated).count();

        double averageHandleHours = workOrders.stream()
            .filter(workOrder -> workOrder.getCompletedAt() != null)
            .mapToLong(workOrder -> Duration.between(workOrder.getCreatedAt(), workOrder.getCompletedAt()).toHours())
            .average()
            .orElse(0D);

        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("total", total);
        statistics.put("pending", pending);
        statistics.put("inProgress", inProgress);
        statistics.put("completed", completed);
        statistics.put("overdue", overdue);
        statistics.put("responseTimeout", responseTimeout);
        statistics.put("responseBreached", responseBreached);
        statistics.put("resolutionBreached", resolutionBreached);
        statistics.put("escalated", escalated);
        statistics.put("averageHandleHours", averageHandleHours);
        statistics.put("statusDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getStatus() == null ? null : workOrder.getStatus().getDescription(), "未设置")));
        statistics.put("priorityDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getPriority() == null ? null : workOrder.getPriority().getDescription(), "未设置")));
        statistics.put("typeDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getType() == null ? null : workOrder.getType().getDescription(), "未分类")));
        statistics.put("sourceDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getSource(), "UNKNOWN")));
        return statistics;
    }

    @Transactional(readOnly = true)
    public String exportWorkOrdersCsv(String keyword,
                                      Long requesterId,
                                      Long assigneeId,
                                      Long departmentId,
                                      Long statusId,
                                      Long priorityId,
                                      Long typeId,
                                      String source,
                                      String customerType,
                                      String productName,
                                      String tag,
                                      Boolean overdue) {
        List<WorkOrder> workOrders = searchWorkOrders(
            keyword, requesterId, assigneeId, departmentId, statusId, priorityId, typeId, source, customerType, productName, tag, overdue
        );
        StringBuilder csv = new StringBuilder();
        csv.append("ID,标题,类型,优先级,状态,提交人,指派人,来源,客户邮箱,创建时间,响应截止,解决截止\n");
        for (WorkOrder workOrder : workOrders) {
            csv.append(workOrder.getId()).append(",")
                .append(csvCell(workOrder.getTitle())).append(",")
                .append(csvCell(workOrder.getType() == null ? "" : workOrder.getType().getDescription())).append(",")
                .append(csvCell(workOrder.getPriority() == null ? "" : workOrder.getPriority().getDescription())).append(",")
                .append(csvCell(workOrder.getStatus() == null ? "" : workOrder.getStatus().getDescription())).append(",")
                .append(csvCell(workOrder.getRequester() == null ? "" : workOrder.getRequester().getName())).append(",")
                .append(csvCell(workOrder.getAssignee() == null ? "" : workOrder.getAssignee().getName())).append(",")
                .append(csvCell(workOrder.getSource())).append(",")
                .append(csvCell(workOrder.getCustomerEmail())).append(",")
                .append(csvCell(formatDate(workOrder.getCreatedAt()))).append(",")
                .append(csvCell(formatDate(workOrder.getResponseDueAt()))).append(",")
                .append(csvCell(formatDate(workOrder.getDueAt())))
                .append("\n");
        }
        return csv.toString();
    }

    @Transactional
    public Optional<WorkOrder> checkAndEscalateSla(WorkOrder workOrder) {
        LocalDateTime now = LocalDateTime.now();
        boolean changed = false;

        if (!workOrder.isResponseBreached()
            && workOrder.getFirstResponseAt() == null
            && workOrder.getResponseDueAt() != null
            && workOrder.getResponseDueAt().isBefore(now)) {
            workOrder.setResponseBreached(true);
            recordHistory(workOrder, workOrder.getRequester(), "SLA_RESPONSE_BREACH", "工单响应超时");
            notificationService.createDualNotification(workOrder, "SLA_RESPONSE_BREACH", "工单响应已超时：" + workOrder.getTitle());
            feishuIntegrationService.notifyWorkOrderEvent("SLA_RESPONSE_BREACH", workOrder, "工单响应超时");
            changed = true;
        }

        if (!workOrder.isResolutionBreached()
            && !isClosed(workOrder)
            && workOrder.getDueAt() != null
            && workOrder.getDueAt().isBefore(now)) {
            workOrder.setResolutionBreached(true);
            recordHistory(workOrder, workOrder.getRequester(), "SLA_RESOLUTION_BREACH", "工单解决超时");
            notificationService.createDualNotification(workOrder, "SLA_RESOLUTION_BREACH", "工单解决已超时：" + workOrder.getTitle());
            feishuIntegrationService.notifyWorkOrderEvent("SLA_RESOLUTION_BREACH", workOrder, "工单解决超时");

            Optional<SlaPolicy> policy = slaPolicyService.matchPolicy(workOrder);
            if (!workOrder.isEscalated() && policy.map(SlaPolicy::isAutoEscalate).orElse(true)) {
                Priority escalationPriority = policy.map(SlaPolicy::getEscalationPriority).orElse(null);
                if (escalationPriority == null) {
                    escalationPriority = priorityRepository.findByName("URGENT").orElse(workOrder.getPriority());
                }
                workOrder.setPriority(escalationPriority);
                workOrder.setEscalated(true);
                recordHistory(workOrder, workOrder.getRequester(), "SLA_ESCALATE", "超时自动升级为 " + escalationPriority.getDescription());
                notificationService.createDualNotification(workOrder, "SLA_ESCALATE", "工单已自动升级：" + workOrder.getTitle());
                feishuIntegrationService.notifyWorkOrderEvent("SLA_ESCALATE", workOrder, "超时自动升级为 " + escalationPriority.getDescription());
            }
            changed = true;
        }

        if (changed) {
            return Optional.of(workOrderRepository.save(workOrder));
        }
        return Optional.empty();
    }

    private void populateWorkOrder(WorkOrder workOrder, WorkOrderRequest request) {
        workOrder.setTitle(request.getTitle().trim());
        workOrder.setDescription(request.getDescription().trim());
        workOrder.setRequester(getRequiredUser(request.getRequesterId()));
        workOrder.setAssignee(request.getAssigneeId() == null ? null : getRequiredUser(request.getAssigneeId()));
        workOrder.setDepartment(getRequiredDepartment(request.getDepartmentId()));

        Status resolvedStatus = resolveStatus(workOrder, request.getStatusId());
        workOrder.setStatus(resolvedStatus);
        workOrder.setPriority(resolvePriority(workOrder, request.getPriorityId()));
        workOrder.setType(resolveType(workOrder, request.getTypeId()));
        workOrder.setSource(normalize(request.getSource()));
        workOrder.setCustomerName(normalize(request.getCustomerName()));
        workOrder.setCustomerEmail(normalize(request.getCustomerEmail()));
        workOrder.setCustomerType(normalize(request.getCustomerType()));
        workOrder.setProductName(normalize(request.getProductName()));
        workOrder.setImpactScope(normalize(request.getImpactScope()));
        workOrder.setTags(normalize(request.getTags()));
        if (request.getResponseDueAt() != null) {
            workOrder.setResponseDueAt(request.getResponseDueAt());
        }
        if (request.getDueAt() != null) {
            workOrder.setDueAt(request.getDueAt());
        }

        if (workOrder.getAssignee() != null && workOrder.getFirstResponseAt() == null) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }
        if (workOrder.getId() == null) {
            workOrder.setResponseBreached(false);
            workOrder.setResolutionBreached(false);
            workOrder.setEscalated(false);
        }
    }

    private Status resolveStatus(WorkOrder workOrder, Long statusId) {
        if (statusId != null) {
            return getRequiredStatus(statusId);
        }
        if (workOrder.getStatus() != null) {
            return workOrder.getStatus();
        }
        return statusRepository.findByName("PENDING")
            .orElseThrow(() -> new EntityNotFoundException("默认状态 PENDING 不存在"));
    }

    private Priority resolvePriority(WorkOrder workOrder, Long priorityId) {
        if (priorityId != null) {
            return getRequiredPriority(priorityId);
        }
        if (workOrder.getPriority() != null) {
            return workOrder.getPriority();
        }
        return priorityRepository.findByName("MEDIUM")
            .orElseThrow(() -> new EntityNotFoundException("默认优先级 MEDIUM 不存在"));
    }

    private WorkOrderType resolveType(WorkOrder workOrder, Long typeId) {
        if (typeId != null) {
            return workOrderTypeRepository.findById(typeId)
                .orElseThrow(() -> new EntityNotFoundException("工单类型不存在，id=" + typeId));
        }
        if (workOrder.getType() != null) {
            return workOrder.getType();
        }
        return workOrderTypeRepository.findByName("DEMAND")
            .orElseGet(() -> workOrderTypeRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("未配置工单类型")));
    }

    private boolean applyAutoAssignmentIfNeeded(WorkOrder workOrder) {
        if (workOrder.getAssignee() != null) {
            return false;
        }
        Optional<AssignmentRule> optionalRule = assignmentRuleService.matchRule(workOrder);
        if (optionalRule.isEmpty()) {
            return false;
        }
        workOrder.setAssignee(optionalRule.get().getTargetAssignee());
        if (workOrder.getFirstResponseAt() == null) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }
        if (workOrder.getStatus() != null && "PENDING".equalsIgnoreCase(workOrder.getStatus().getName())) {
            statusRepository.findByName("IN_PROGRESS").ifPresent(workOrder::setStatus);
        }
        return true;
    }

    private void applySlaDeadlineIfMissing(WorkOrder workOrder) {
        Optional<SlaPolicy> policyOptional = slaPolicyService.matchPolicy(workOrder);
        int responseHours = policyOptional.map(SlaPolicy::getResponseHours).orElseGet(() -> defaultResponseHours(workOrder));
        int resolveHours = policyOptional.map(SlaPolicy::getResolveHours).orElseGet(() -> defaultResolveHours(workOrder));

        LocalDateTime baseTime = workOrder.getCreatedAt() == null ? LocalDateTime.now() : workOrder.getCreatedAt();
        if (workOrder.getResponseDueAt() == null) {
            workOrder.setResponseDueAt(baseTime.plusHours(responseHours));
        }
        if (workOrder.getDueAt() == null) {
            workOrder.setDueAt(baseTime.plusHours(resolveHours));
        }
    }

    private int defaultResponseHours(WorkOrder workOrder) {
        String priorityName = workOrder.getPriority() == null ? "" : workOrder.getPriority().getName();
        if ("URGENT".equalsIgnoreCase(priorityName)) {
            return 1;
        }
        if ("HIGH".equalsIgnoreCase(priorityName)) {
            return 4;
        }
        if ("MEDIUM".equalsIgnoreCase(priorityName)) {
            return 8;
        }
        return 24;
    }

    private int defaultResolveHours(WorkOrder workOrder) {
        String priorityName = workOrder.getPriority() == null ? "" : workOrder.getPriority().getName();
        if ("URGENT".equalsIgnoreCase(priorityName)) {
            return 8;
        }
        if ("HIGH".equalsIgnoreCase(priorityName)) {
            return 24;
        }
        if ("MEDIUM".equalsIgnoreCase(priorityName)) {
            return 48;
        }
        return 72;
    }

    private void validateStatusTransition(String currentStatusName, String targetStatusName) {
        if (!StringUtils.hasText(targetStatusName) || !StringUtils.hasText(currentStatusName) || currentStatusName.equalsIgnoreCase(targetStatusName)) {
            return;
        }
        Set<String> allowedTransitions = STATUS_TRANSITIONS.getOrDefault(currentStatusName.toUpperCase(), Collections.emptySet());
        if (!allowedTransitions.contains(targetStatusName.toUpperCase())) {
            throw new IllegalArgumentException("非法状态流转：" + currentStatusName + " -> " + targetStatusName);
        }
    }

    private void applyStatusLifecycle(WorkOrder workOrder, String statusName, String previousStatusName) {
        LocalDateTime now = LocalDateTime.now();
        if ("COMPLETED".equalsIgnoreCase(statusName)) {
            if (workOrder.getResolvedAt() == null) {
                workOrder.setResolvedAt(now);
            }
            if (workOrder.getCompletedAt() == null) {
                workOrder.setCompletedAt(now);
            }
            if (workOrder.getClosedAt() == null) {
                workOrder.setClosedAt(now);
            }
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(statusName)) {
            if (workOrder.getClosedAt() == null) {
                workOrder.setClosedAt(now);
            }
            return;
        }
        if ("IN_PROGRESS".equalsIgnoreCase(statusName) && workOrder.getFirstResponseAt() == null) {
            workOrder.setFirstResponseAt(now);
        }
        if ("COMPLETED".equalsIgnoreCase(previousStatusName) || "CANCELLED".equalsIgnoreCase(previousStatusName)) {
            workOrder.setClosedAt(null);
            workOrder.setResolvedAt(null);
            workOrder.setCompletedAt(null);
        }
    }

    private int calculateTotalScore(RequirementEvaluationRequest request) {
        return request.getRequirementValue()
            + request.getCustomerWeight()
            + request.getCompetitorImpact()
            + request.getImpactScopeScore()
            - request.getDevelopmentEffort();
    }

    private String mapPriorityByScore(int totalScore) {
        if (totalScore >= 14) {
            return "URGENT";
        }
        if (totalScore >= 11) {
            return "HIGH";
        }
        if (totalScore >= 8) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private void recordHistory(WorkOrder workOrder, User user, String action, String description) {
        WorkOrderHistory history = new WorkOrderHistory();
        history.setWorkOrder(workOrder);
        history.setUser(user);
        history.setAction(action);
        history.setDescription(description);
        workOrderHistoryRepository.save(history);
    }

    private WorkOrder getRequiredWorkOrder(Long workOrderId) {
        return workOrderRepository.findById(workOrderId)
            .orElseThrow(() -> new EntityNotFoundException("工单不存在，id=" + workOrderId));
    }

    private User getRequiredUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("用户不存在，id=" + userId));
    }

    private Department getRequiredDepartment(Long departmentId) {
        return departmentRepository.findById(departmentId)
            .orElseThrow(() -> new EntityNotFoundException("部门不存在，id=" + departmentId));
    }

    private Status getRequiredStatus(Long statusId) {
        return statusRepository.findById(statusId)
            .orElseThrow(() -> new EntityNotFoundException("工单状态不存在，id=" + statusId));
    }

    private Priority getRequiredPriority(Long priorityId) {
        return priorityRepository.findById(priorityId)
            .orElseThrow(() -> new EntityNotFoundException("优先级不存在，id=" + priorityId));
    }

    private User resolveOperator(Long operatorId, User fallback) {
        if (operatorId != null) {
            return getRequiredUser(operatorId);
        }
        if (fallback != null) {
            return fallback;
        }
        throw new EntityNotFoundException("缺少操作人信息");
    }

    private void validateCustomerAccess(WorkOrder workOrder, String customerEmail) {
        if (!StringUtils.hasText(customerEmail)) {
            throw new IllegalArgumentException("客户邮箱不能为空");
        }
        if (!StringUtils.hasText(workOrder.getCustomerEmail())
            || !workOrder.getCustomerEmail().trim().equalsIgnoreCase(customerEmail.trim())) {
            throw new IllegalArgumentException("客户无权访问该工单");
        }
    }

    private boolean hasStatus(WorkOrder workOrder, String statusName) {
        return workOrder.getStatus() != null && statusName.equalsIgnoreCase(workOrder.getStatus().getName());
    }

    private boolean isClosed(WorkOrder workOrder) {
        return hasStatus(workOrder, "COMPLETED") || hasStatus(workOrder, "CANCELLED");
    }

    private Map<String, Long> groupCount(List<WorkOrder> workOrders, Function<WorkOrder, String> classifier) {
        return workOrders.stream()
            .collect(Collectors.groupingBy(classifier, LinkedHashMap::new, Collectors.counting()));
    }

    private String getLabel(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return StringUtils.hasText(source) && source.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private String abbreviate(String content) {
        if (!StringUtils.hasText(content) || content.length() <= 80) {
            return content;
        }
        return content.substring(0, 77) + "...";
    }

    private String csvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String formatDate(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
