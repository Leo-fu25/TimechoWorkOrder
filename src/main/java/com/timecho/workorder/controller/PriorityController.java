package com.timecho.workorder.controller;

import com.timecho.workorder.model.Priority;
import com.timecho.workorder.service.PriorityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/priorities")
public class PriorityController {
    @Autowired
    private PriorityService priorityService;
    
    @PostMapping
    public ResponseEntity<Priority> createPriority(@RequestBody Priority priority) {
        Priority createdPriority = priorityService.createPriority(priority);
        return new ResponseEntity<>(createdPriority, HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Priority> getPriorityById(@PathVariable Long id) {
        Optional<Priority> priority = priorityService.getPriorityById(id);
        return priority.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<Priority>> getAllPriorities() {
        List<Priority> priorities = priorityService.getAllPriorities();
        return ResponseEntity.ok(priorities);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Priority> updatePriority(@PathVariable Long id, @RequestBody Priority priority) {
        Priority updatedPriority = priorityService.updatePriority(id, priority);
        return updatedPriority != null ? ResponseEntity.ok(updatedPriority) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePriority(@PathVariable Long id) {
        priorityService.deletePriority(id);
        return ResponseEntity.noContent().build();
    }
}