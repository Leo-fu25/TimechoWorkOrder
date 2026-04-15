package com.timecho.workorder.controller;

import com.timecho.workorder.model.SlaPolicy;
import com.timecho.workorder.service.SlaPolicyService;
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
@RequestMapping("/api/sla-policies")
public class SlaPolicyController {
    @Autowired
    private SlaPolicyService slaPolicyService;

    @PostMapping
    public ResponseEntity<SlaPolicy> createPolicy(@RequestBody SlaPolicy policy) {
        return new ResponseEntity<>(slaPolicyService.createPolicy(policy), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SlaPolicy> updatePolicy(@PathVariable Long id, @RequestBody SlaPolicy policy) {
        SlaPolicy updated = slaPolicyService.updatePolicy(id, policy);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<SlaPolicy>> getAllPolicies() {
        return ResponseEntity.ok(slaPolicyService.getAllPolicies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SlaPolicy> getPolicyById(@PathVariable Long id) {
        Optional<SlaPolicy> policy = slaPolicyService.getPolicyById(id);
        return policy.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        slaPolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
