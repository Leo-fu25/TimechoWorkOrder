package com.timecho.workorder.repository;

import com.timecho.workorder.model.WorkOrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkOrderNotificationRepository extends JpaRepository<WorkOrderNotification, Long> {
    List<WorkOrderNotification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFlagFalse(Long recipientId);

    List<WorkOrderNotification> findByRecipientIdAndReadFlagFalse(Long recipientId);

    void deleteByWorkOrderId(Long workOrderId);
}
