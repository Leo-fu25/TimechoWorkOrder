package com.timecho.workorder.repository;

import com.timecho.workorder.model.WorkOrderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkOrderTypeRepository extends JpaRepository<WorkOrderType, Long> {
    Optional<WorkOrderType> findByName(String name);
}
