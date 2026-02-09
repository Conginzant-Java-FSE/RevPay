package com.revpay.model;

import com.revpay.config.AuditConfig;
import com.revpay.enums.RecordStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "personal_profile")
public class PersonalProfile extends AuditConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long profileId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String fullName;

    private LocalDate dob;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private RecordStatus status;

    public Long getProfile_id() {
        return profileId;
    }

    public void setProfile_id(Long profile_id) {
        this.profileId = profile_id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
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

    public PersonalProfile(Long id, User user, String fullName, LocalDate dob, String address, RecordStatus status) {
        this.profileId = id;
        this.user = user;
        this.fullName = fullName;
        this.dob = dob;
        this.address = address;
        this.status = status;
    }

    public PersonalProfile() {}
}
