package com.timecho.workorder.controller;

import com.timecho.workorder.model.WorkOrderNotification;
import com.timecho.workorder.service.WorkOrderNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private WorkOrderNotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<WorkOrderNotification>> getNotifications(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(@RequestParam Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("unreadCount", notificationService.countUnread(userId));
        return ResponseEntity.ok(data);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<WorkOrderNotification> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(@RequestParam Long userId) {
        int updated = notificationService.markAllRead(userId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("updatedCount", updated);
        return ResponseEntity.ok(data);
    }
}
