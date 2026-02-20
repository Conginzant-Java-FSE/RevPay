package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.*;

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

    @Column(columnDefinition = "TEXT")
    private String invoiceDetails;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public String getInvoiceDetails() {
        return invoiceDetails;
    }

    public void setInvoiceDetails(String invoiceDetails) {
        this.invoiceDetails = invoiceDetails;
    }

    public BusinessProfile(Long businessId, User user, String businessName, String taxId, String address,
            String invoiceDetails, RecordStatus status) {
        this.businessId = businessId;
        this.user = user;
        this.businessName = businessName;
        this.taxId = taxId;
        this.address = address;
        this.invoiceDetails = invoiceDetails;
        this.status = status;
    }

    public BusinessProfile() {
    }
}
