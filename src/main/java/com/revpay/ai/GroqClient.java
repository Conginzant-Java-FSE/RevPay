package com.revpay.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that calls the Groq API (OpenAI-compatible).
 *
 * Endpoint : https://api.groq.com/openai/v1/chat/completions
 * Model    : llama-3.1-8b-instant
 * Auth     : Bearer token — read from application.properties (groq.api.key)
 *
 * Conversation memory: the full history is passed as prior messages so the
 * model understands the context of each follow-up question.
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL    = "llama-3.1-8b-instant";

    @Value("${groq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Send a full conversation to Groq and return the model's text reply.
     *
     * @param systemPrompt  System context (live user data + persona)
     * @param history       Previous turns in this session (may be null/empty)
     * @param userMessage   The current user message
     * @return              The AI-generated reply text
     */
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Build message list: [system] + [history turns] + [current user message]
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. System prompt (always first)
        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // 2. Prior conversation turns (gives the model memory)
        if (history != null) {
            for (ChatMessage turn : history) {
                // Skip the last user message — we'll add it fresh below
                Map<String, Object> msg = new HashMap<>();
                msg.put("role",    turn.getRole());
                msg.put("content", turn.getContent());
                messages.add(msg);
            }
        }

        // 3. Current user message
        Map<String, Object> currentMsg = new HashMap<>();
        currentMsg.put("role",    "user");
        currentMsg.put("content", userMessage);
        messages.add(currentMsg);

        Map<String, Object> body = new HashMap<>();
        body.put("model",    MODEL);
        body.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        log.debug("Groq request: {} messages (including {} history turns)",
                messages.size(), history != null ? history.size() : 0);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(GROQ_URL, request, Map.class);

            if (response.getBody() == null) {
                throw new RuntimeException("Groq API returned empty response body");
            }

            // Extract choices[0].message.content
            List<?> choices = (List<?>) response.getBody().get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RuntimeException("Groq API returned no choices");
            }

            Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
            Map<?, ?> message     = (Map<?, ?>) firstChoice.get("message");
            String content        = (String) message.get("content");

            log.debug("Groq response received ({} chars)", content != null ? content.length() : 0);
            return content != null ? content
                    : "Sorry, I could not generate a response. Please try again.";

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "AI service is temporarily unavailable. Please try again later.");
        }
    }
}
