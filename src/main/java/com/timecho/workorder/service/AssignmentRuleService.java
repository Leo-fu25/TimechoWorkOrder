package com.timecho.workorder.service;

import com.timecho.workorder.model.AssignmentRule;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.repository.AssignmentRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class AssignmentRuleService {
    @Autowired
    private AssignmentRuleRepository assignmentRuleRepository;

    public AssignmentRule createRule(AssignmentRule rule) {
        return assignmentRuleRepository.save(rule);
    }

    public AssignmentRule updateRule(Long id, AssignmentRule request) {
        Optional<AssignmentRule> existing = assignmentRuleRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        AssignmentRule rule = existing.get();
        rule.setName(request.getName());
        rule.setDepartment(request.getDepartment());
        rule.setType(request.getType());
        rule.setSource(request.getSource());
        rule.setCustomerType(request.getCustomerType());
        rule.setTargetAssignee(request.getTargetAssignee());
        rule.setPriority(request.getPriority());
        rule.setActive(request.isActive());
        return assignmentRuleRepository.save(rule);
    }

    public List<AssignmentRule> getAllRules() {
        return assignmentRuleRepository.findAll();
    }

    public Optional<AssignmentRule> getRuleById(Long id) {
        return assignmentRuleRepository.findById(id);
    }

    public void deleteRule(Long id) {
        assignmentRuleRepository.deleteById(id);
    }

    public Optional<AssignmentRule> matchRule(WorkOrder workOrder) {
        return assignmentRuleRepository.findByActiveTrueOrderByPriorityAscIdAsc().stream()
            .filter(rule -> matchDepartment(rule, workOrder))
            .filter(rule -> matchType(rule, workOrder))
            .filter(rule -> matchSource(rule, workOrder))
            .filter(rule -> matchCustomerType(rule, workOrder))
            .findFirst();
    }

    private boolean matchDepartment(AssignmentRule rule, WorkOrder workOrder) {
        return rule.getDepartment() == null
            || (workOrder.getDepartment() != null && rule.getDepartment().getId().equals(workOrder.getDepartment().getId()));
    }

    private boolean matchType(AssignmentRule rule, WorkOrder workOrder) {
        return rule.getType() == null
            || (workOrder.getType() != null && rule.getType().getId().equals(workOrder.getType().getId()));
    }

    private boolean matchSource(AssignmentRule rule, WorkOrder workOrder) {
        return !StringUtils.hasText(rule.getSource())
            || (StringUtils.hasText(workOrder.getSource()) && rule.getSource().trim().equalsIgnoreCase(workOrder.getSource().trim()));
    }

    private boolean matchCustomerType(AssignmentRule rule, WorkOrder workOrder) {
        return !StringUtils.hasText(rule.getCustomerType())
            || (StringUtils.hasText(workOrder.getCustomerType()) && rule.getCustomerType().trim().equalsIgnoreCase(workOrder.getCustomerType().trim()));
    }
}
