package com.timecho.workorder.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feishu_push_logs")
public class FeishuPushLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String webhookName;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column
    private Long workOrderId;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 512)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
