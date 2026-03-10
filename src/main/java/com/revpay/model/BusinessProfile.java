package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.BusinessType;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "business_profile")
public class BusinessProfile extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long businessId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String businessName;

    private String taxId;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    private BusinessType businessType;

    private String contact_phone;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String invoiceDetails;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

}
