package com.timecho.workorder.controller;

import com.timecho.workorder.model.WorkOrderType;
import com.timecho.workorder.service.WorkOrderTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/workorder-types")
public class WorkOrderTypeController {
    @Autowired
    private WorkOrderTypeService workOrderTypeService;

    @PostMapping
    public ResponseEntity<WorkOrderType> createType(@RequestBody WorkOrderType workOrderType) {
        WorkOrderType createdType = workOrderTypeService.createType(workOrderType);
        return new ResponseEntity<>(createdType, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderType> getTypeById(@PathVariable Long id) {
        Optional<WorkOrderType> workOrderType = workOrderTypeService.getTypeById(id);
        return workOrderType.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<WorkOrderType>> getAllTypes() {
        return ResponseEntity.ok(workOrderTypeService.getAllTypes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkOrderType> updateType(@PathVariable Long id, @RequestBody WorkOrderType workOrderType) {
        WorkOrderType updatedType = workOrderTypeService.updateType(id, workOrderType);
        return updatedType != null ? ResponseEntity.ok(updatedType) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable Long id) {
        workOrderTypeService.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}
