package com.timecho.workorder.service;

import com.timecho.workorder.model.Department;
import com.timecho.workorder.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;
    
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }
    
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }
    
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }
    
    public Department updateDepartment(Long id, Department updatedDepartment) {
        Optional<Department> existingDepartment = departmentRepository.findById(id);
        if (existingDepartment.isPresent()) {
            Department department = existingDepartment.get();
            department.setName(updatedDepartment.getName());
            department.setDescription(updatedDepartment.getDescription());
            department.setActive(updatedDepartment.isActive());
            return departmentRepository.save(department);
        }
        return null;
    }
    
    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }
}