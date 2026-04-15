package com.timecho.workorder.repository;

import com.timecho.workorder.model.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long>, JpaSpecificationExecutor<WorkOrder> {
    List<WorkOrder> findByRequesterId(Long requesterId);
    List<WorkOrder> findByAssigneeId(Long assigneeId);
    List<WorkOrder> findByDepartmentId(Long departmentId);
    List<WorkOrder> findByStatusId(Long statusId);
    List<WorkOrder> findByPriorityId(Long priorityId);

    List<WorkOrder> findByCustomerEmailIgnoreCaseOrderByCreatedAtDesc(String customerEmail);
}
