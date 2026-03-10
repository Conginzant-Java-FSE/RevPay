package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    private String purpose;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime expiresAt;

}
