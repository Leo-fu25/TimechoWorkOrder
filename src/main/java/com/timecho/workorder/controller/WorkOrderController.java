package com.timecho.workorder.controller;

import com.timecho.workorder.dto.AssignWorkOrderRequest;
import com.timecho.workorder.dto.AttachmentRequest;
import com.timecho.workorder.dto.BulkStatusUpdateRequest;
import com.timecho.workorder.dto.RequirementEvaluationRequest;
import com.timecho.workorder.dto.WorkOrderCommentRequest;
import com.timecho.workorder.dto.WorkOrderRequest;
import com.timecho.workorder.dto.WorkOrderStatusRequest;
import com.timecho.workorder.model.RequirementEvaluation;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.model.WorkOrderAttachment;
import com.timecho.workorder.model.WorkOrderComment;
import com.timecho.workorder.model.WorkOrderHistory;
import com.timecho.workorder.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/workorders")
public class WorkOrderController {
    @Autowired
    private WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrder> createWorkOrder(@Valid @RequestBody WorkOrderRequest request) {
        WorkOrder createdWorkOrder = workOrderService.createWorkOrder(request);
        return new ResponseEntity<>(createdWorkOrder, HttpStatus.CREATED);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(workOrderService.getStatistics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getWorkOrderById(@PathVariable Long id) {
        Optional<WorkOrder> workOrder = workOrderService.getWorkOrderById(id);
        return workOrder.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WorkOrder>> getAllWorkOrders(@RequestParam(required = false) String keyword,
                                                            @RequestParam(required = false) Long requesterId,
                                                            @RequestParam(required = false) Long assigneeId,
                                                            @RequestParam(required = false) Long departmentId,
                                                            @RequestParam(required = false) Long statusId,
                                                            @RequestParam(required = false) Long priorityId,
                                                            @RequestParam(required = false) Long typeId,
                                                            @RequestParam(required = false) String source,
                                                            @RequestParam(required = false) String customerType,
                                                            @RequestParam(required = false) String productName,
                                                            @RequestParam(required = false) String tag,
                                                            @RequestParam(required = false) Boolean overdue) {
        List<WorkOrder> workOrders = workOrderService.searchWorkOrders(
            keyword, requesterId, assigneeId, departmentId, statusId, priorityId, typeId, source, customerType, productName, tag, overdue
        );
        return ResponseEntity.ok(workOrders);
    }

    @GetMapping("/requester/{requesterId}")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersByRequesterId(@PathVariable Long requesterId) {
        return ResponseEntity.ok(workOrderService.searchWorkOrders(null, requesterId, null, null, null, null, null, null, null, null, null, null));
    }

    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersByAssigneeId(@PathVariable Long assigneeId) {
        return ResponseEntity.ok(workOrderService.searchWorkOrders(null, null, assigneeId, null, null, null, null, null, null, null, null, null));
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<WorkOrder>> getWorkOrdersByDepartmentId(@PathVariable Long departmentId) {
        return ResponseEntity.ok(workOrderService.searchWorkOrders(null, null, null, departmentId, null, null, null, null, null, null, null, null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrder> updateWorkOrder(@PathVariable Long id, @Valid @RequestBody WorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.updateWorkOrder(id, request));
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<WorkOrder> assignWorkOrder(@PathVariable Long id, @Valid @RequestBody AssignWorkOrderRequest request) {
        return ResponseEntity.ok(workOrderService.assignWorkOrder(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<WorkOrder> updateWorkOrderStatus(@PathVariable Long id, @Valid @RequestBody WorkOrderStatusRequest request) {
        return ResponseEntity.ok(workOrderService.updateStatus(id, request));
    }

    @PostMapping("/batch/status")
    public ResponseEntity<List<WorkOrder>> bulkUpdateStatus(@Valid @RequestBody BulkStatusUpdateRequest request) {
        return ResponseEntity.ok(workOrderService.bulkUpdateStatus(request));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<WorkOrderComment> addComment(@PathVariable Long id, @Valid @RequestBody WorkOrderCommentRequest request) {
        return new ResponseEntity<>(workOrderService.addComment(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<WorkOrderComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getComments(id));
    }

    @PostMapping("/{id}/attachments")
    public ResponseEntity<WorkOrderAttachment> addAttachment(@PathVariable Long id, @Valid @RequestBody AttachmentRequest request) {
        return new ResponseEntity<>(workOrderService.addAttachment(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<WorkOrderAttachment>> getAttachments(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getAttachments(id));
    }

    @PostMapping("/{id}/evaluations")
    public ResponseEntity<RequirementEvaluation> evaluateRequirement(@PathVariable Long id,
                                                                    @Valid @RequestBody RequirementEvaluationRequest request) {
        return new ResponseEntity<>(workOrderService.evaluateRequirement(id, request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}/evaluations")
    public ResponseEntity<List<RequirementEvaluation>> getEvaluations(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getEvaluations(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkOrder(@PathVariable Long id) {
        workOrderService.deleteWorkOrder(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WorkOrderHistory>> getWorkOrderHistory(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getWorkOrderHistory(id));
    }
}
