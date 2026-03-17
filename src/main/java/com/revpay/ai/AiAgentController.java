package com.revpay.ai;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the RevPay AI Assistant.
 *
 * Endpoint : POST /api/ai/chat
 * Auth     : JWT required — enforced automatically by SecurityConfig
 *            (anyRequest().authenticated() covers all /api/** routes)
 *
 * Request  : { "message": "Why did my payment fail?" }
 * Response : { "response": "Payments may fail due to ..." }
 */
@RestController
@RequestMapping("/api/ai")
public class AiAgentController {

    private static final Logger log = LoggerFactory.getLogger(AiAgentController.class);

    @Autowired
    private AiAgentService aiAgentService;

    /**
     * Chat with the RevPay AI assistant.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("POST /api/ai/chat");
        String reply = aiAgentService.chat(request.getMessage(), request.getHistory());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    // ── Exception handler ────────────────────────────────────────────────────

    /**
     * Catches validation errors (blank message) and any service/Groq errors,
     * and returns a clean JSON error body instead of a raw 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleException(Exception ex) {
        log.error("AI chat error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .internalServerError()
                .body(new ChatResponse(
                        "Sorry, something went wrong: " + ex.getMessage() +
                        ". Please try again later."));
    }
}
