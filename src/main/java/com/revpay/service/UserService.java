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
import java.util.List;
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

    public UserService(UserRepository userRepository, PersonalProfileRepository personalProfileRepository,
                       BankAccountRepository bankAccountRepository, BCryptPasswordEncoder passwordEncoder, JwtUtil jwtUtil)
    {
        this.userRepository = userRepository;
        this.personalProfileRepository = personalProfileRepository;
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

        return new UserRegistrationResponse("User registered successfully");
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
                user.getAccountType().toString()
        );

        // Return login response
        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAccountType());
    }

    // verify and change the users password
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

        logger.info("Password reset successfully for: {}", user.getEmail());
        return new ForgotPasswordResponse("Password reset successfully", true);
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

    // Get the authendicated user
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
        bankAccount.setIsPrimary(request.getIsPrimary());
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

}