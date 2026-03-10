package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.BankAccountType;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bank_accounts")
public class BankAccount extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long accountId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String accountHolderName;

    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    private String ifscCode;

    private Boolean isPrimary;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private BankAccountType accountType;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;


}