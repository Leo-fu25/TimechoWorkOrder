package com.timecho.workorder.service;

import com.timecho.workorder.model.Priority;
import com.timecho.workorder.repository.PriorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PriorityService {
    @Autowired
    private PriorityRepository priorityRepository;
    
    public Priority createPriority(Priority priority) {
        return priorityRepository.save(priority);
    }
    
    public Optional<Priority> getPriorityById(Long id) {
        return priorityRepository.findById(id);
    }

    public Optional<Priority> getPriorityByName(String name) {
        return priorityRepository.findByName(name);
    }
    
    public List<Priority> getAllPriorities() {
        return priorityRepository.findAll();
    }
    
    public Priority updatePriority(Long id, Priority updatedPriority) {
        Optional<Priority> existingPriority = priorityRepository.findById(id);
        if (existingPriority.isPresent()) {
            Priority priority = existingPriority.get();
            priority.setName(updatedPriority.getName());
            priority.setDescription(updatedPriority.getDescription());
            priority.setLevel(updatedPriority.getLevel());
            priority.setActive(updatedPriority.isActive());
            return priorityRepository.save(priority);
        }
        return null;
    }
    
    public void deletePriority(Long id) {
        priorityRepository.deleteById(id);
    }
}
