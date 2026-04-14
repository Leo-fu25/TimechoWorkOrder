package com.timecho.workorder.service;

import com.timecho.workorder.dto.AssignWorkOrderRequest;
import com.timecho.workorder.dto.AttachmentRequest;
import com.timecho.workorder.dto.BulkStatusUpdateRequest;
import com.timecho.workorder.dto.RequirementEvaluationRequest;
import com.timecho.workorder.dto.WorkOrderCommentRequest;
import com.timecho.workorder.dto.WorkOrderRequest;
import com.timecho.workorder.dto.WorkOrderStatusRequest;
import com.timecho.workorder.model.Department;
import com.timecho.workorder.model.Priority;
import com.timecho.workorder.model.RequirementEvaluation;
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
import com.timecho.workorder.repository.WorkOrderRepository;
import com.timecho.workorder.repository.WorkOrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {
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
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private PriorityRepository priorityRepository;

    @Autowired
    private WorkOrderTypeRepository workOrderTypeRepository;

    @Transactional
    public WorkOrder createWorkOrder(WorkOrderRequest request) {
        WorkOrder workOrder = new WorkOrder();
        populateWorkOrder(workOrder, request);
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        recordHistory(savedWorkOrder, resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester()), "CREATE", "工单创建");
        return savedWorkOrder;
    }

    @Transactional(readOnly = true)
    public Optional<WorkOrder> getWorkOrderById(Long id) {
        return workOrderRepository.findById(id);
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

    @Transactional
    public WorkOrder updateWorkOrder(Long id, WorkOrderRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        populateWorkOrder(workOrder, request);
        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        recordHistory(savedWorkOrder, resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester()), "UPDATE", "工单基础信息更新");
        return savedWorkOrder;
    }

    @Transactional
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
        return savedWorkOrder;
    }

    @Transactional
    public WorkOrder updateStatus(Long id, WorkOrderStatusRequest request) {
        WorkOrder workOrder = getRequiredWorkOrder(id);
        Status status = getRequiredStatus(request.getStatusId());
        workOrder.setStatus(status);
        applyStatusLifecycle(workOrder, status.getName());
        if (workOrder.getFirstResponseAt() == null && !"PENDING".equalsIgnoreCase(status.getName())) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }

        WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
        String description = StringUtils.hasText(request.getRemark())
            ? "工单状态更新为 " + status.getDescription() + "，备注：" + request.getRemark().trim()
            : "工单状态更新为 " + status.getDescription();
        recordHistory(savedWorkOrder, resolveOperator(request.getOperatorId(), savedWorkOrder.getRequester()), "STATUS_CHANGE", description);
        return savedWorkOrder;
    }

    @Transactional
    public List<WorkOrder> bulkUpdateStatus(BulkStatusUpdateRequest request) {
        Status status = getRequiredStatus(request.getStatusId());
        User explicitOperator = request.getOperatorId() == null ? null : getRequiredUser(request.getOperatorId());

        return request.getWorkOrderIds().stream()
            .map(workOrderId -> {
                WorkOrder workOrder = getRequiredWorkOrder(workOrderId);
                workOrder.setStatus(status);
                applyStatusLifecycle(workOrder, status.getName());
                WorkOrder savedWorkOrder = workOrderRepository.save(workOrder);
                User operator = explicitOperator != null ? explicitOperator : savedWorkOrder.getRequester();
                String description = StringUtils.hasText(request.getRemark())
                    ? "批量更新状态为 " + status.getDescription() + "，备注：" + request.getRemark().trim()
                    : "批量更新状态为 " + status.getDescription();
                recordHistory(savedWorkOrder, operator, "BULK_STATUS_CHANGE", description);
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
        recordHistory(workOrder, user, "COMMENT", visibility + "：" + abbreviate(request.getContent().trim()));
        return savedComment;
    }

    @Transactional(readOnly = true)
    public List<WorkOrderComment> getComments(Long workOrderId) {
        getRequiredWorkOrder(workOrderId);
        return workOrderCommentRepository.findByWorkOrderIdOrderByCreatedAtAsc(workOrderId);
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

        recordHistory(workOrder, user, "ATTACHMENT", "上传附件：" + savedAttachment.getFileName());
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
        recordHistory(workOrder, evaluator, "EVALUATION", "需求评估完成，总分：" + totalScore);
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
        statistics.put("averageHandleHours", averageHandleHours);
        statistics.put("statusDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getStatus() == null ? null : workOrder.getStatus().getDescription(), "未设置")));
        statistics.put("priorityDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getPriority() == null ? null : workOrder.getPriority().getDescription(), "未设置")));
        statistics.put("typeDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getType() == null ? null : workOrder.getType().getDescription(), "未分类")));
        statistics.put("sourceDistribution", groupCount(workOrders, workOrder -> getLabel(workOrder.getSource(), "UNKNOWN")));
        return statistics;
    }

    private void populateWorkOrder(WorkOrder workOrder, WorkOrderRequest request) {
        workOrder.setTitle(request.getTitle().trim());
        workOrder.setDescription(request.getDescription().trim());
        workOrder.setRequester(getRequiredUser(request.getRequesterId()));
        workOrder.setAssignee(request.getAssigneeId() == null ? null : getRequiredUser(request.getAssigneeId()));
        workOrder.setDepartment(getRequiredDepartment(request.getDepartmentId()));
        workOrder.setStatus(resolveStatus(request.getStatusId()));
        workOrder.setPriority(resolvePriority(request.getPriorityId()));
        workOrder.setType(resolveType(request.getTypeId()));
        workOrder.setSource(normalize(request.getSource()));
        workOrder.setCustomerName(normalize(request.getCustomerName()));
        workOrder.setCustomerEmail(normalize(request.getCustomerEmail()));
        workOrder.setCustomerType(normalize(request.getCustomerType()));
        workOrder.setProductName(normalize(request.getProductName()));
        workOrder.setImpactScope(normalize(request.getImpactScope()));
        workOrder.setTags(normalize(request.getTags()));
        workOrder.setResponseDueAt(request.getResponseDueAt());
        workOrder.setDueAt(request.getDueAt());

        if (workOrder.getAssignee() != null && workOrder.getFirstResponseAt() == null) {
            workOrder.setFirstResponseAt(LocalDateTime.now());
        }
        applyStatusLifecycle(workOrder, workOrder.getStatus().getName());
    }

    private Status resolveStatus(Long statusId) {
        if (statusId != null) {
            return getRequiredStatus(statusId);
        }
        return statusRepository.findByName("PENDING")
            .orElseThrow(() -> new EntityNotFoundException("默认状态 PENDING 不存在"));
    }

    private Priority resolvePriority(Long priorityId) {
        if (priorityId != null) {
            return getRequiredPriority(priorityId);
        }
        return priorityRepository.findByName("MEDIUM")
            .orElseThrow(() -> new EntityNotFoundException("默认优先级 MEDIUM 不存在"));
    }

    private WorkOrderType resolveType(Long typeId) {
        if (typeId == null) {
            return workOrderTypeRepository.findByName("DEMAND")
                .orElseGet(() -> workOrderTypeRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("未配置工单类型")));
        }
        return workOrderTypeRepository.findById(typeId)
            .orElseThrow(() -> new EntityNotFoundException("工单类型不存在，id=" + typeId));
    }

    private void applyStatusLifecycle(WorkOrder workOrder, String statusName) {
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

    private String abbreviate(String content) {
        if (content.length() <= 80) {
            return content;
        }
        return content.substring(0, 77) + "...";
    }
}
