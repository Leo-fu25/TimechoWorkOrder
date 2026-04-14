package com.timecho.workorder.repository;

import com.timecho.workorder.model.WorkOrderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderHistoryRepository extends JpaRepository<WorkOrderHistory, Long> {
    List<WorkOrderHistory> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);

    void deleteByWorkOrderId(Long workOrderId);
}
