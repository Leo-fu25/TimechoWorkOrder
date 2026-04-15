package com.timecho.workorder.controller;

import com.timecho.workorder.dto.FeishuTestMessageRequest;
import com.timecho.workorder.model.FeishuPushLog;
import com.timecho.workorder.model.FeishuWebhookConfig;
import com.timecho.workorder.service.FeishuIntegrationService;
import com.timecho.workorder.service.FeishuWebhookConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/integrations/feishu")
public class FeishuIntegrationController {
    @Autowired
    private FeishuWebhookConfigService configService;

    @Autowired
    private FeishuIntegrationService integrationService;

    @GetMapping("/webhooks")
    public ResponseEntity<List<FeishuWebhookConfig>> getAllWebhooks() {
        return ResponseEntity.ok(configService.getAll());
    }

    @GetMapping("/webhooks/{id}")
    public ResponseEntity<FeishuWebhookConfig> getWebhook(@PathVariable Long id) {
        Optional<FeishuWebhookConfig> webhook = configService.getById(id);
        return webhook.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/webhooks")
    public ResponseEntity<FeishuWebhookConfig> createWebhook(@RequestBody FeishuWebhookConfig config) {
        return new ResponseEntity<>(configService.create(config), HttpStatus.CREATED);
    }

    @PutMapping("/webhooks/{id}")
    public ResponseEntity<FeishuWebhookConfig> updateWebhook(@PathVariable Long id, @RequestBody FeishuWebhookConfig config) {
        FeishuWebhookConfig updated = configService.update(id, config);
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(updated);
    }

    @DeleteMapping("/webhooks/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/webhooks/{id}/test")
    public ResponseEntity<Map<String, Object>> testWebhook(@PathVariable Long id, @Valid @RequestBody FeishuTestMessageRequest request) {
        boolean success = integrationService.sendTestMessage(id, request.getMessage());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("webhookId", id);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<FeishuPushLog>> getRecentLogs() {
        return ResponseEntity.ok(integrationService.getRecentLogs());
    }
}
