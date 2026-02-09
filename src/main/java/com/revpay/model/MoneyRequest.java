package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.RequestStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "money_requests")
public class MoneyRequest extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "requestee_id")
    private User requestee;

    private BigDecimal amount;

    private String purpose;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getRequestee() {
        return requestee;
    }

    public void setRequestee(User requestee) {
        this.requestee = requestee;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public MoneyRequest(Long requestId, User requester, User requestee, BigDecimal amount, String purpose, RequestStatus status) {
        this.requestId = requestId;
        this.requester = requester;
        this.requestee = requestee;
        this.amount = amount;
        this.purpose = purpose;
        this.status = status;
    }

    public MoneyRequest() {}
}
