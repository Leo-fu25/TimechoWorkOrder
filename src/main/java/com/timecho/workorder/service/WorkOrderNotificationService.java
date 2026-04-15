package com.timecho.workorder.service;

import com.timecho.workorder.model.User;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.model.WorkOrderNotification;
import com.timecho.workorder.repository.WorkOrderNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class WorkOrderNotificationService {
    @Autowired
    private WorkOrderNotificationRepository notificationRepository;

    public void createNotification(WorkOrder workOrder, User recipient, String eventType, String message) {
        if (recipient == null) {
            return;
        }
        WorkOrderNotification notification = new WorkOrderNotification();
        notification.setWorkOrder(workOrder);
        notification.setRecipient(recipient);
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setReadFlag(false);
        notificationRepository.save(notification);
    }

    public void createDualNotification(WorkOrder workOrder, String eventType, String message) {
        createNotification(workOrder, workOrder.getRequester(), eventType, message);
        if (workOrder.getAssignee() != null && !workOrder.getAssignee().getId().equals(workOrder.getRequester().getId())) {
            createNotification(workOrder, workOrder.getAssignee(), eventType, message);
        }
    }

    public List<WorkOrderNotification> getUserNotifications(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFlagFalse(userId);
    }

    public WorkOrderNotification markRead(Long notificationId) {
        Optional<WorkOrderNotification> optional = notificationRepository.findById(notificationId);
        if (optional.isEmpty()) {
            throw new EntityNotFoundException("通知不存在，id=" + notificationId);
        }
        WorkOrderNotification notification = optional.get();
        notification.setReadFlag(true);
        return notificationRepository.save(notification);
    }

    public int markAllRead(Long userId) {
        List<WorkOrderNotification> notifications = notificationRepository.findByRecipientIdAndReadFlagFalse(userId);
        notifications.forEach(notification -> notification.setReadFlag(true));
        notificationRepository.saveAll(notifications);
        return notifications.size();
    }
}
