package com.timecho.workorder.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timecho.workorder.model.FeishuPushLog;
import com.timecho.workorder.model.FeishuWebhookConfig;
import com.timecho.workorder.model.WorkOrder;
import com.timecho.workorder.repository.FeishuPushLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FeishuIntegrationService {
    @Autowired
    private FeishuWebhookConfigService configService;

    @Autowired
    private FeishuPushLogRepository logRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Shanghai"));

    public List<FeishuPushLog> getRecentLogs() {
        return logRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public boolean sendTestMessage(Long configId, String message) {
        Optional<FeishuWebhookConfig> config = configService.getById(configId);
        if (config.isEmpty()) {
            throw new IllegalArgumentException("飞书Webhook配置不存在，id=" + configId);
        }
        return push(config.get(), "TEST", null, "【测试消息】" + message.trim());
    }

    public void notifyWorkOrderEvent(String eventType, WorkOrder workOrder, String summary) {
        for (FeishuWebhookConfig config : configService.getEnabled()) {
            if (!acceptEvent(config, eventType)) {
                continue;
            }
            String text = formatWorkOrderMessage(eventType, workOrder, summary);
            push(config, eventType, workOrder, text);
        }
    }

    private boolean push(FeishuWebhookConfig config, String eventType, WorkOrder workOrder, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "text");
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("text", text);
        payload.put("content", content);

        long timestamp = Instant.now().getEpochSecond();
        if (StringUtils.hasText(config.getSecret())) {
            payload.put("timestamp", String.valueOf(timestamp));
            payload.put("sign", buildSign(timestamp, config.getSecret()));
        }

        String requestBody = toJson(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        FeishuPushLog pushLog = new FeishuPushLog();
        pushLog.setWebhookName(config.getName());
        pushLog.setEventType(eventType);
        pushLog.setWorkOrderId(workOrder == null ? null : workOrder.getId());
        pushLog.setRequestBody(requestBody);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(config.getWebhookUrl(), entity, String.class);
            pushLog.setSuccess(response.getStatusCode().is2xxSuccessful());
            pushLog.setResponseBody(response.getBody());
            if (!pushLog.isSuccess()) {
                pushLog.setErrorMessage("HTTP " + response.getStatusCodeValue());
            }
            logRepository.save(pushLog);
            return pushLog.isSuccess();
        } catch (Exception exception) {
            pushLog.setSuccess(false);
            pushLog.setErrorMessage(exception.getClass().getSimpleName() + ": " + exception.getMessage());
            logRepository.save(pushLog);
            return false;
        }
    }

    private boolean acceptEvent(FeishuWebhookConfig config, String eventType) {
        if ("CREATE".equalsIgnoreCase(eventType)) {
            return config.isNotifyCreate();
        }
        if ("STATUS_CHANGE".equalsIgnoreCase(eventType) || "ASSIGN".equalsIgnoreCase(eventType) || "BULK_STATUS_CHANGE".equalsIgnoreCase(eventType)) {
            return config.isNotifyStatus();
        }
        if ("COMMENT".equalsIgnoreCase(eventType) || "CUSTOMER_COMMENT".equalsIgnoreCase(eventType) || "ATTACHMENT".equalsIgnoreCase(eventType)) {
            return config.isNotifyComment();
        }
        if ("EVALUATION".equalsIgnoreCase(eventType)) {
            return config.isNotifyEvaluation();
        }
        if (eventType != null && eventType.startsWith("SLA_")) {
            return config.isNotifySla();
        }
        return true;
    }

    private String formatWorkOrderMessage(String eventType, WorkOrder workOrder, String summary) {
        StringBuilder text = new StringBuilder();
        text.append("【Timecho工单通知】").append(eventType).append("\n");
        if (workOrder != null) {
            text.append("工单: #").append(workOrder.getId()).append(" ").append(safe(workOrder.getTitle())).append("\n");
            text.append("状态: ").append(workOrder.getStatus() == null ? "-" : safe(workOrder.getStatus().getDescription())).append(" | ");
            text.append("优先级: ").append(workOrder.getPriority() == null ? "-" : safe(workOrder.getPriority().getDescription())).append("\n");
            text.append("客户: ").append(safe(workOrder.getCustomerName())).append(" <").append(safe(workOrder.getCustomerEmail())).append(">\n");
        }
        text.append("说明: ").append(summary == null ? "-" : summary).append("\n");
        text.append("时间: ").append(DT.format(Instant.now()));
        return text.toString();
    }

    private String safe(String value) {
        return value == null ? "-" : value;
    }

    private String buildSign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            return Base64.getEncoder().encodeToString(signData);
        } catch (Exception exception) {
            return "";
        }
    }

    private String toJson(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化飞书请求失败", exception);
        }
    }
}
