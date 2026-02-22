package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.CardType;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "card_token", nullable = false)
    private String cardToken;

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type")
    private CardType cardType;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    private String nickname;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    // Billing address stored as structured fields
    @Column(name = "billing_street")
    private String billingStreet;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_state")
    private String billingState;

    @Column(name = "billing_zip")
    private String billingZip;

    @Column(name = "billing_country")
    private String billingCountry = "US";

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}