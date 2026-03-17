package com.revpay.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public AI endpoint — no JWT required.
 *
 * Endpoint: POST /api/ai/public-chat
 *
 * This endpoint answers general RevPay questions (platform FAQ, features,
 * how things work, refund policies, etc.) WITHOUT accessing any personal
 * user data. It is safe to expose publicly.
 *
 * Personal data endpoints remain at POST /api/ai/chat (JWT required).
 */
@RestController
@RequestMapping("/api/ai")
public class PublicAiController {

    private static final Logger log = LoggerFactory.getLogger(PublicAiController.class);

    @Autowired
    private GroqClient groqClient;

    private static final String PUBLIC_SYSTEM_PROMPT =
            "You are RevPay Assistant, a friendly and knowledgeable AI representative " +
            "for RevPay — a modern digital fintech platform.\n\n" +
            "RevPay offers:\n" +
            "- Digital wallet with instant money transfers\n" +
            "- Multi-card and bank account linking\n" +
            "- Business invoicing and payment acceptance\n" +
            "- Loan management with EMI tracking\n" +
            "- Smart analytics and spending insights\n" +
            "- Real-time notifications\n" +
            "- Bank-grade security with JWT auth, 2FA, and transaction PINs\n\n" +
            "You help PROSPECTIVE and EXISTING users with:\n" +
            "- How RevPay works and what features it offers\n" +
            "- How to register and get started\n" +
            "- Refund and support policies\n" +
            "- How wallets, transactions, loans, and invoices work\n" +
            "- General fintech and payment advice\n\n" +
            "IMPORTANT RESTRICTIONS:\n" +
            "- You do NOT have access to any personal account data here.\n" +
            "- If the user asks about their specific balance, transactions, or account details, " +
            "  politely tell them to log in and use the in-app AI assistant for personalised help.\n" +
            "- You CANNOT perform any actions (like sending money or creating accounts). " +
            "  If asked to do so, politely explain that you can only provide guidance.\n" +
            "- Always be concise, friendly, and professional.\n" +
            "- Use the ₹ symbol for Indian Rupee amounts.";


    /**
     * Public chat — answers general RevPay questions without any user data.
     *
     * Request:  { "message": "How do I register?", "history": [...] }
     * Response: { "response": "You can register by..." }
     */
    @PostMapping("/public-chat")
    public ResponseEntity<ChatResponse> publicChat(@RequestBody ChatRequest request) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please provide a message."));
        }

        log.info("Public AI chat request received");

        List<ChatMessage> history = request.getHistory();
        String reply = groqClient.chat(PUBLIC_SYSTEM_PROMPT, history, request.getMessage());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleException(Exception ex) {
        log.error("Public AI chat error: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError()
                .body(new ChatResponse(
                        "Sorry, I'm temporarily unavailable. Please try again shortly."));
    }
}
