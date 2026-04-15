package com.timecho.workorder.controller;

import com.timecho.workorder.model.AssignmentRule;
import com.timecho.workorder.service.AssignmentRuleService;
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
@RequestMapping("/api/assignment-rules")
public class AssignmentRuleController {
    @Autowired
    private AssignmentRuleService assignmentRuleService;

    @PostMapping
    public ResponseEntity<AssignmentRule> createRule(@RequestBody AssignmentRule rule) {
        return new ResponseEntity<>(assignmentRuleService.createRule(rule), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssignmentRule> updateRule(@PathVariable Long id, @RequestBody AssignmentRule rule) {
        AssignmentRule updated = assignmentRuleService.updateRule(id, rule);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<List<AssignmentRule>> getAllRules() {
        return ResponseEntity.ok(assignmentRuleService.getAllRules());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssignmentRule> getRuleById(@PathVariable Long id) {
        Optional<AssignmentRule> rule = assignmentRuleService.getRuleById(id);
        return rule.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        assignmentRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
