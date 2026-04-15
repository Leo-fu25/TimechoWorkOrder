package com.timecho.workorder.repository;

import com.timecho.workorder.model.FeishuWebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeishuWebhookConfigRepository extends JpaRepository<FeishuWebhookConfig, Long> {
    List<FeishuWebhookConfig> findByEnabledTrueOrderByIdAsc();
}
