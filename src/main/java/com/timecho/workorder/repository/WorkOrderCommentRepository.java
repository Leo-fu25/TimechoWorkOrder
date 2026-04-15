package com.timecho.workorder.repository;

import com.timecho.workorder.model.WorkOrderComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderCommentRepository extends JpaRepository<WorkOrderComment, Long> {
    List<WorkOrderComment> findByWorkOrderIdOrderByCreatedAtAsc(Long workOrderId);

    List<WorkOrderComment> findByWorkOrderIdAndInternalOnlyFalseOrderByCreatedAtAsc(Long workOrderId);

    void deleteByWorkOrderId(Long workOrderId);
}
