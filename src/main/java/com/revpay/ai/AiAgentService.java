package com.revpay.ai;

import com.revpay.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the AI assistant:
 *  1. Gather live user context via AgentToolService
 *  2. Build a RevPay-branded system prompt with the context
 *  3. Forward the full conversation history + new message to GroqClient
 *  4. Return the AI reply
 */
@Service
public class AiAgentService extends BaseService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);

    @Autowired
    private GroqClient groqClient;

    @Autowired
    private AgentToolService agentToolService;

    private static final String SYSTEM_PROMPT =
            "You are RevPay Assistant, a knowledgeable and friendly financial support AI " +
            "for RevPay — a digital fintech platform. You help users with:\n" +
            "- Wallet balance and top-up queries\n" +
            "- Transaction history and payment failures\n" +
            "- Loan applications, EMI schedules, and repayments\n" +
            "- Invoice creation and management (business accounts)\n" +
            "- Refund policies and general platform guidance\n\n" +
            "CRITICAL RESTRICTION:\n" +
            "- You are a read-only guide. You CANNOT perform any actions on behalf of the user.\n" +
            "- If the user asks you to send money, pay a bill, approve a loan, or change a setting, " +
            "you must politely explain that you cannot perform actions and guide them on how to do it themselves in the app.\n\n" +
            "Always be concise, accurate, and professional. " +
            "Use the ₹ symbol for Indian Rupee amounts.\n" +
            "Remember what the user said earlier in this conversation — " +
            "you have full access to the chat history.\n\n" +
            "── LIVE USER ACCOUNT CONTEXT ───────────────────────────────────\n" +
            "%s\n" +
            "%s\n" +
            "%s\n" +
            "%s\n" +

            "────────────────────────────────────────────────────────────────\n\n" +
            "Use the above data to personalise your answers when relevant. " +
            "Never reveal raw stack traces or internal system errors.";

    /**
     * Main entry point called by AiAgentController.
     *
     * @param userMessage  The user's current message
     * @param history      All prior turns in this session (for conversation memory)
     * @return             The AI-generated response string
     */
    public String chat(String userMessage, List<ChatMessage> history) {

        log.info("AI chat request from: {}", getLoggedInUser().getEmail());

        // Step 1 — gather live context (each call handles its own errors)
        String walletInfo  = agentToolService.getWalletBalance();
        String txnInfo     = agentToolService.getRecentTransactions();
        String loanInfo    = agentToolService.getLoanBalance();
        String invoiceInfo = agentToolService.getInvoiceSummary();

        // Step 2 — inject context into system prompt
        String systemPrompt = String.format(
                SYSTEM_PROMPT, walletInfo, txnInfo, loanInfo, invoiceInfo);

        // Step 3 — call Groq with full history so it remembers prior messages
        return groqClient.chat(systemPrompt, history, userMessage);
    }
}
