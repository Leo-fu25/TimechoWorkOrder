package com.timecho.workorder.service;

import com.timecho.workorder.model.SlaPolicy;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.repository.SlaPolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SlaPolicyService {
    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    public SlaPolicy createPolicy(SlaPolicy policy) {
        return slaPolicyRepository.save(policy);
    }

    public SlaPolicy updatePolicy(Long id, SlaPolicy request) {
        Optional<SlaPolicy> existing = slaPolicyRepository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        SlaPolicy policy = existing.get();
        policy.setName(request.getName());
        policy.setDepartment(request.getDepartment());
        policy.setType(request.getType());
        policy.setPriority(request.getPriority());
        policy.setResponseHours(request.getResponseHours());
        policy.setResolveHours(request.getResolveHours());
        policy.setAutoEscalate(request.isAutoEscalate());
        policy.setEscalationPriority(request.getEscalationPriority());
        policy.setActive(request.isActive());
        return slaPolicyRepository.save(policy);
    }

    public List<SlaPolicy> getAllPolicies() {
        return slaPolicyRepository.findAll();
    }

    public Optional<SlaPolicy> getPolicyById(Long id) {
        return slaPolicyRepository.findById(id);
    }

    public void deletePolicy(Long id) {
        slaPolicyRepository.deleteById(id);
    }

    public Optional<SlaPolicy> matchPolicy(WorkOrder workOrder) {
        return slaPolicyRepository.findByActiveTrueOrderByIdAsc().stream()
            .filter(policy -> matchDepartment(policy, workOrder))
            .filter(policy -> matchType(policy, workOrder))
            .filter(policy -> matchPriority(policy, workOrder))
            .findFirst();
    }

    private boolean matchDepartment(SlaPolicy policy, WorkOrder workOrder) {
        return policy.getDepartment() == null
            || (workOrder.getDepartment() != null && policy.getDepartment().getId().equals(workOrder.getDepartment().getId()));
    }

    private boolean matchType(SlaPolicy policy, WorkOrder workOrder) {
        return policy.getType() == null
            || (workOrder.getType() != null && policy.getType().getId().equals(workOrder.getType().getId()));
    }

    private boolean matchPriority(SlaPolicy policy, WorkOrder workOrder) {
        return policy.getPriority() == null
            || (workOrder.getPriority() != null && policy.getPriority().getId().equals(workOrder.getPriority().getId()));
    }
}
