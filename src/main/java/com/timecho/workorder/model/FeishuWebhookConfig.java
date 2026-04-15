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
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "feishu_webhook_configs")
public class FeishuWebhookConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(nullable = false, length = 1024)
    private String webhookUrl;

    @Column(length = 256)
    private String secret;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private boolean notifyCreate;

    @Column(nullable = false)
    private boolean notifyStatus;

    @Column(nullable = false)
    private boolean notifyComment;

    @Column(nullable = false)
    private boolean notifyEvaluation;

    @Column(nullable = false)
    private boolean notifySla;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
