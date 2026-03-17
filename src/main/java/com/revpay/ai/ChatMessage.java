package com.revpay.ai;

/**
 * A single turn in the conversation history.
 * role must be "user" or "assistant" — matches the Groq API message format.
 */
public class ChatMessage {

    private String role;    // "user" | "assistant"
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role    = role;
        this.content = content;
    }

    public String getRole()    { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
