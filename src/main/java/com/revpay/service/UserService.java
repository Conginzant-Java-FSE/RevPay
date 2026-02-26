package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.enums.*;
import com.revpay.exception.*;
import com.revpay.model.*;
import com.revpay.repository.*;
import com.revpay.util.JwtUtil;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PersonalProfileRepository personalProfileRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BusinessProfileRepository businessProfileRepository;

    public UserService(UserRepository userRepository, PersonalProfileRepository personalProfileRepository,
            BusinessProfileRepository businessProfileRepository,
            BankAccountRepository bankAccountRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.personalProfileRepository = personalProfileRepository;
        this.businessProfileRepository = businessProfileRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // New User Register
    @Transactional
    public UserRegistrationResponse register(UserRegistrationRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new UserRegistrationResponse("Email already registered");
        }

        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return new UserRegistrationResponse("Phone number already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setSecurityQuestion(request.getSecurityQuestion());
        user.setSecurityAnswer(passwordEncoder.encode(request.getSecurityAnswer()));
        user.setAccountType(request.getAccountType() != null ? request.getAccountType() : AccountType.PERSONAL);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        createWalletForUser(savedUser);

        createWelcomeNotification(savedUser);

        logger.info("User registered successfully and welcome notification created for {}", savedUser.getEmail());

        return new UserRegistrationResponse("User registered successfully");
    }

    // create welcome notification for new users
    private void createWelcomeNotification(User user) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage("Welcome to RevPay! Your wallet has been created successfully.");
        notification.setType(NotificationType.GENERAL);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
    }

    // Create a wallet for the user after registration
    private void createWalletForUser(User user) {

        if (walletRepository.existsByUser(user)) {
            throw new IllegalStateException("Wallet already exists for this user");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setCurrency("INR"); // default currency

        walletRepository.save(wallet);

        logger.info("Wallet created for user: {}", user.getEmail());
    }

    // User Login method
    public LoginResponse login(LoginRequest request) {
        // Find user by email or phone
        User user = (User) userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        // Check if account is active
        if (!user.isActive()) {
            throw new RuntimeException("Account is inactive");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getAccountType().toString());

        // Return login response
        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAccountType());
    }

    // verify and change the users password
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        logger.info("Forgot password request for: {}", request.getEmailOrPhone());

        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .or(() -> userRepository.findByPhone(request.getEmailOrPhone()))
                .orElseThrow(() -> {
                    logger.warn("Forgot password failed - user not found: {}", request.getEmailOrPhone());
                    return new UserNotFoundException("No user found with the provided email or phone number");
                });

        if (!user.isActive()) {
            logger.warn("Forgot password failed - account inactive for: {}", request.getEmailOrPhone());
            throw new RuntimeException("Account is inactive. Cannot reset password.");
        }

        if (!user.getSecurityQuestion().equalsIgnoreCase(request.getSecurityQuestion())) {
            logger.warn("Forgot password failed - security question mismatch for: {}", request.getEmailOrPhone());
            throw new SecurityAnswerMismatchException("Security question does not match");
        }

        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            logger.warn("Forgot password failed - security answer mismatch for: {}", request.getEmailOrPhone());
            throw new SecurityAnswerMismatchException("Security answer is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        createPasswordChangedNotification(user);

        logger.info("Password reset successfully for: {}", user.getEmail());
        return new ForgotPasswordResponse("Password reset successfully", true);
    }

    // Password updated notification
    private void createPasswordChangedNotification(User user) {

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(
                "Your password was changed successfully. If this wasn't you, please contact support immediately.");
        notification.setType(NotificationType.SECURITY_ALERT);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);

        logger.info("Password change notification created for: {}", user.getEmail());
    }

    // get all users list
    public List<UserListResponse> getAllUsers() {
        List<User> users = userRepository.findAll();

        return users.stream().map(user -> {
            UserListResponse response = new UserListResponse();
            response.setUserId(user.getId());
            response.setFullName(user.getFullName());
            response.setEmail(user.getEmail());
            response.setPhone(user.getPhone());
            response.setAccountType(user.getAccountType());
            return response;
        }).collect(Collectors.toList());
    }

    // Fetch user profile details
    public ProfileResponse getProfileByEmail(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        ProfileResponse profile = new ProfileResponse();
        profile.setUserId(user.getId());
        profile.setFullName(user.getFullName());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setAccountType(user.getAccountType());

        return profile;
    }

    // Get the authenticated user
    private User getLoggedInUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // create personal profile with bank details
    @Transactional
    public void createPersonalProfileWithBank(PersonalProfileFullRequest request) {

        User user = getLoggedInUser();

        if (personalProfileRepository.existsByUser(user)) {
            throw new IllegalStateException("Personal profile already exists for this user");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
        }

        PersonalProfile profile = new PersonalProfile();
        profile.setUser(user);
        profile.setDob(request.getDob());
        profile.setAddress(request.getAddress());
        profile.setStatus(RecordStatus.ACTIVE);
        personalProfileRepository.save(profile);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setIfscCode(request.getIfscCode());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : true);
        bankAccount.setStatus(RecordStatus.ACTIVE);
        bankAccountRepository.save(bankAccount);
    }

    // update personal profile with bank details
    @Transactional
    public void updatePersonalProfileWithBank(PersonalProfileFullRequest request) {

        User user = getLoggedInUser();

        logger.info("Updating profile for user: {}", user.getEmail());

        PersonalProfile profile = personalProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Personal profile does not exist for this user"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
            logger.info("Username updated");
        }

        if (request.getDob() != null) {
            profile.setDob(request.getDob());
        }

        if (request.getAddress() != null) {
            profile.setAddress(request.getAddress());
        }

        personalProfileRepository.save(profile);
        logger.info("Personal profile updated");

        BankAccount bankAccount = bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> new IllegalStateException("Primary bank account not found"));

        if (request.getAccountHolderName() != null) {
            bankAccount.setAccountHolderName(request.getAccountHolderName());
        }

        if (request.getBankName() != null) {
            bankAccount.setBankName(request.getBankName());
        }

        if (request.getAccountNumber() != null) {
            bankAccount.setAccountNumber(request.getAccountNumber());
        }

        if (request.getIfscCode() != null) {
            bankAccount.setIfscCode(request.getIfscCode());
        }

        bankAccountRepository.save(bankAccount);

        logger.info("Bank account updated for user: {}", user.getEmail());
    }

    // set MTPIN for users
    @Transactional
    public void setTransactionPin(SetTransactionPinRequest request) {

        User user = getLoggedInUser();

        if (user.getMtPin() != null) {
            throw new IllegalStateException("Transaction PIN already set");
        }

        if (!request.getMtPin().equals(request.getConfirmMtPin())) {
            throw new IllegalArgumentException("MTPIN and Confirm MTPIN do not match");
        }

        String encodedPin = passwordEncoder.encode(request.getMtPin());

        user.setMtPin(encodedPin);
        userRepository.save(user);

        logger.info("Transaction PIN set for user: {}", user.getEmail());
    }

    // create business profile with bank details
    @Transactional
    public void createBusinessProfileWithBank(BusinessProfileFullRequest request) {

        User user = getLoggedInUser();

        if (businessProfileRepository.existsByUser(user)) {
            logger.warn("Business profile already exists for user: {}", user.getEmail());
            throw new IllegalStateException("Business profile already exists for this user");
        }

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
            userRepository.save(user);
        }

        BusinessProfile profile = new BusinessProfile();
        profile.setUser(user);
        profile.setBusinessName(request.getBusinessName());
        profile.setBusinessType(request.getBusinessType());
        profile.setTaxId(request.getTaxId());
        profile.setAddress(request.getAddress());
        profile.setContact_phone(request.getContactPhone());
        profile.setWebsite(request.getWebsite());
        profile.setStatus(RecordStatus.ACTIVE); // requires admin approval

        businessProfileRepository.save(profile);

        BankAccount bankAccount = new BankAccount();
        bankAccount.setUser(user);
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setIfscCode(request.getIfscCode());
        bankAccount.setAccountType(request.getAccountType());
        bankAccount.setIsPrimary(request.getIsPrimary() != null ? request.getIsPrimary() : true);
        bankAccount.setStatus(RecordStatus.ACTIVE);

        bankAccountRepository.save(bankAccount);

        logger.info("Business profile and bank account created for user: {}", user.getEmail());
    }

    public SecurityQuestionResponse getSecurityQuestion() {

        User user = getLoggedInUser();

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            throw new IllegalStateException("No security question set for this user");
        }

        return new SecurityQuestionResponse(user.getSecurityQuestion());
    }

    public ProfileMeResponse getFullProfile() {

        User user = getLoggedInUser();

        BigDecimal walletBalance = walletRepository.findByUser(user)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);

        ProfileMeResponse.BankAccountSummary bankAccountSummary = bankAccountRepository.findByUserAndIsPrimaryTrue(user)
                .map(bank -> ProfileMeResponse.BankAccountSummary.builder()
                        .bankName(bank.getBankName())
                        .accountNumber(maskAccountNumber(bank.getAccountNumber()))
                        .accountType(bank.getAccountType() != null
                                ? bank.getAccountType().name()
                                : null)
                        .build())
                .orElse(null);

        ProfileMeResponse.ProfileMeResponseBuilder builder = ProfileMeResponse.builder()
                .userId(user.getId())
                .accountType(user.getAccountType())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .walletBalance(walletBalance)
                .bankAccount(bankAccountSummary)
                .createdAt(user.getCreatedAt());

        if (user.getAccountType() == AccountType.PERSONAL) {

            Optional<PersonalProfile> personalOpt = personalProfileRepository.findByUser(user);

            boolean profileComplete = personalOpt.isPresent();
            builder.profileComplete(profileComplete);

            personalOpt.ifPresent(profile -> {
                builder.address(profile.getAddress());
                builder.dob(profile.getDob() != null
                        ? profile.getDob().toString()
                        : null);
            });
        }

        if (user.getAccountType() == AccountType.BUSINESS) {

            Optional<BusinessProfile> businessOpt = businessProfileRepository.findByUser(user);

            boolean profileComplete = businessOpt.isPresent();
            builder.profileComplete(profileComplete);

            businessOpt.ifPresent(profile -> {
                builder.businessName(profile.getBusinessName());
                builder.businessType(profile.getBusinessType() != null
                        ? profile.getBusinessType().name()
                        : null);
                builder.taxId(profile.getTaxId());
                builder.contactPhone(profile.getContact_phone());
                builder.website(profile.getWebsite());
                builder.businessStatus(profile.getStatus() != null
                        ? profile.getStatus().name()
                        : null);
            });
        }

        return builder.build();
    }

    // Mask account number — show only last 4 digits
    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return accountNumber;
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    @Transactional
    public void updateBusinessProfile(BusinessProfileUpdateRequest request) {

        User user = getLoggedInUser();

        if (user.getAccountType() != AccountType.BUSINESS) {
            throw new IllegalStateException("Only business users can update business profile");
        }

        BusinessProfile profile = businessProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Business profile does not exist"));

        if (request.getBusinessName() != null) {
            profile.setBusinessName(request.getBusinessName());
        }

        if (request.getBusinessType() != null) {
            profile.setBusinessType(request.getBusinessType());
        }

        if (request.getTaxId() != null) {
            profile.setTaxId(request.getTaxId());
        }

        if (request.getContactPhone() != null) {
            profile.setContact_phone(request.getContactPhone());
        }

        if (request.getWebsite() != null) {
            profile.setWebsite(request.getWebsite());
        }

        businessProfileRepository.save(profile);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        User user = getLoggedInUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        createPasswordChangedNotification(user);

        logger.info("Password changed successfully for user: {}", user.getEmail());
    }

    @Transactional
    public void changeTransactionPin(ChangePinRequest request) {
        User user = getLoggedInUser();

        if (user.getMtPin() != null) {
            if (request.getCurrentPin() == null || request.getCurrentPin().isBlank()) {
                throw new IllegalArgumentException("Current PIN is required to update transaction PIN");
            }
            if (!passwordEncoder.matches(request.getCurrentPin(), user.getMtPin())) {
                throw new IllegalArgumentException("Current transaction PIN is incorrect");
            }
        }

        if (!request.getNewPin().equals(request.getConfirmPin())) {
            throw new IllegalArgumentException("New PIN and confirm PIN do not match");
        }

        user.setMtPin(passwordEncoder.encode(request.getNewPin()));
        userRepository.save(user);

        logger.info("Transaction PIN updated successfully for user: {}", user.getEmail());
    }

    @Transactional
    public void updateSecurityQuestion(UpdateSecurityQuestionRequest request) {

        User user = getLoggedInUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setSecurityQuestion(request.getSecurityQuestion());

        String normalizedAnswer = request.getSecurityAnswer().toLowerCase().trim();
        user.setSecurityAnswer(passwordEncoder.encode(normalizedAnswer));

        userRepository.save(user);

        logger.info("Security question updated for user: {}", user.getEmail());
    }

}