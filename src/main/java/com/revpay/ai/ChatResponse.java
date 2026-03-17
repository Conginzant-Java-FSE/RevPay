package com.revpay.ai;

/**
 * Response body for POST /api/ai/chat
 *
 * Example: { "response": "Payments may fail due to ..." }
 */
public class ChatResponse {

    private String response;

    public ChatResponse() {}

    public ChatResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
