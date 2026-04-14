package com.timecho.workorder.service;

import com.timecho.workorder.model.WorkOrderType;
import com.timecho.workorder.repository.WorkOrderTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkOrderTypeService {
    @Autowired
    private WorkOrderTypeRepository workOrderTypeRepository;

    public WorkOrderType createType(WorkOrderType workOrderType) {
        return workOrderTypeRepository.save(workOrderType);
    }

    public Optional<WorkOrderType> getTypeById(Long id) {
        return workOrderTypeRepository.findById(id);
    }

    public Optional<WorkOrderType> getTypeByName(String name) {
        return workOrderTypeRepository.findByName(name);
    }

    public List<WorkOrderType> getAllTypes() {
        return workOrderTypeRepository.findAll();
    }

    public WorkOrderType updateType(Long id, WorkOrderType updatedType) {
        Optional<WorkOrderType> existingType = workOrderTypeRepository.findById(id);
        if (existingType.isPresent()) {
            WorkOrderType workOrderType = existingType.get();
            workOrderType.setName(updatedType.getName());
            workOrderType.setDescription(updatedType.getDescription());
            workOrderType.setActive(updatedType.isActive());
            return workOrderTypeRepository.save(workOrderType);
        }
        return null;
    }

    public void deleteType(Long id) {
        workOrderTypeRepository.deleteById(id);
    }
}
