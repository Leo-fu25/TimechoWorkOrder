package com.timecho.workorder.service;

import com.timecho.workorder.model.FeishuWebhookConfig;
import com.timecho.workorder.repository.FeishuWebhookConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FeishuWebhookConfigService {
    @Autowired
    private FeishuWebhookConfigRepository repository;

    public FeishuWebhookConfig create(FeishuWebhookConfig config) {
        normalizeConfig(config);
        return repository.save(config);
    }

    public FeishuWebhookConfig update(Long id, FeishuWebhookConfig request) {
        Optional<FeishuWebhookConfig> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return null;
        }
        FeishuWebhookConfig config = existing.get();
        config.setName(request.getName());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setEnabled(request.isEnabled());
        config.setNotifyCreate(request.isNotifyCreate());
        config.setNotifyStatus(request.isNotifyStatus());
        config.setNotifyComment(request.isNotifyComment());
        config.setNotifyEvaluation(request.isNotifyEvaluation());
        config.setNotifySla(request.isNotifySla());
        normalizeConfig(config);
        return repository.save(config);
    }

    public List<FeishuWebhookConfig> getAll() {
        return repository.findAll();
    }

    public List<FeishuWebhookConfig> getEnabled() {
        return repository.findByEnabledTrueOrderByIdAsc();
    }

    public Optional<FeishuWebhookConfig> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    private void normalizeConfig(FeishuWebhookConfig config) {
        if (config.getName() != null) {
            config.setName(config.getName().trim());
        }
        if (config.getWebhookUrl() != null) {
            config.setWebhookUrl(config.getWebhookUrl().trim());
        }
        if (config.getSecret() != null) {
            String secret = config.getSecret().trim();
            config.setSecret(secret.isEmpty() ? null : secret);
        }
    }
}
