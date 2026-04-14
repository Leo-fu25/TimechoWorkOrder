package com.timecho.workorder.service;

import com.timecho.workorder.model.Status;
import com.timecho.workorder.repository.StatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StatusService {
    @Autowired
    private StatusRepository statusRepository;
    
    public Status createStatus(Status status) {
        return statusRepository.save(status);
    }
    
    public Optional<Status> getStatusById(Long id) {
        return statusRepository.findById(id);
    }

    public Optional<Status> getStatusByName(String name) {
        return statusRepository.findByName(name);
    }
    
    public List<Status> getAllStatuses() {
        return statusRepository.findAll();
    }
    
    public Status updateStatus(Long id, Status updatedStatus) {
        Optional<Status> existingStatus = statusRepository.findById(id);
        if (existingStatus.isPresent()) {
            Status status = existingStatus.get();
            status.setName(updatedStatus.getName());
            status.setDescription(updatedStatus.getDescription());
            status.setActive(updatedStatus.isActive());
            return statusRepository.save(status);
        }
        return null;
    }
    
    public void deleteStatus(Long id) {
        statusRepository.deleteById(id);
    }
}
