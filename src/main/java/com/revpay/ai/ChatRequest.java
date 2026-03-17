package com.revpay.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request body for POST /api/ai/chat
 *
 * Example:
 * {
 *   "message": "What was my last transaction?",
 *   "history": [
 *     { "role": "user",      "content": "What is my balance?" },
 *     { "role": "assistant", "content": "Your wallet balance is ₹1,250.00 INR." }
 *   ]
 * }
 *
 * history is optional — omit or send [] for the first message.
 */
public class ChatRequest {

    @NotBlank(message = "Message must not be blank")
    private String message;

    /** Previous turns in this session (role = "user" or "assistant") */
    private List<ChatMessage> history;

    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<ChatMessage> getHistory() { return history; }
    public void setHistory(List<ChatMessage> history) { this.history = history; }
}
