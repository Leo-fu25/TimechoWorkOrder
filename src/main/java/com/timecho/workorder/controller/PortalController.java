package com.timecho.workorder.controller;

import com.timecho.workorder.dto.PortalCommentRequest;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.model.WorkOrderComment;
import com.timecho.workorder.service.WorkOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/portal/workorders")
public class PortalController {
    @Autowired
    private WorkOrderService workOrderService;

    @GetMapping
    public ResponseEntity<List<WorkOrder>> searchPortalWorkOrders(@RequestParam String customerEmail,
                                                                  @RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) Long statusId) {
        return ResponseEntity.ok(workOrderService.searchPortalWorkOrders(customerEmail, keyword, statusId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrder> getPortalWorkOrder(@PathVariable Long id, @RequestParam String customerEmail) {
        Optional<WorkOrder> workOrder = workOrderService.getPortalWorkOrderById(id, customerEmail);
        return workOrder.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<WorkOrderComment>> getPortalComments(@PathVariable Long id, @RequestParam String customerEmail) {
        return ResponseEntity.ok(workOrderService.getPortalComments(id, customerEmail));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<WorkOrderComment> addPortalComment(@PathVariable Long id, @Valid @RequestBody PortalCommentRequest request) {
        return new ResponseEntity<>(workOrderService.addPortalComment(id, request), HttpStatus.CREATED);
    }
}
