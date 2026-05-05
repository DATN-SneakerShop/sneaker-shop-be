package com.sneakershop.backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    @Value("${telegram.message-thread-id:}")
    private String messageThreadId;

    public void sendMessage(String text) {
        if (!enabled) {
            return;
        }
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.warn("Telegram chưa cấu hình đủ bot-token/chat-id");
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");

        if (messageThreadId != null && !messageThreadId.isBlank()) {
            body.put("message_thread_id", Integer.parseInt(messageThreadId));
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("Telegram response: {}", response.getBody());
        } catch (Exception e) {
            log.error("Gửi Telegram thất bại", e);
        }
    }
}