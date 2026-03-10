package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.enums.*;
import com.revpay.model.*;
import com.revpay.repository.MoneyRequestRepository;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import com.revpay.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class MoneyRequestService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(MoneyRequestService.class);

    @Autowired
    private MoneyRequestRepository moneyRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void cancelRequest(Long requestId, String email) {

        User requester = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        MoneyRequest request = moneyRequestRepository.findByRequestIdAndRequester(requestId, requester)
                .orElseThrow(() -> new RuntimeException("Request not found or not yours"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }

        request.setStatus(RequestStatus.CANCELLED);

        moneyRequestRepository.save(request);
    }

    @Transactional
    public MoneyRequestCreateResponse createRequest(MoneyRequestCreateRequest dto) {

        User requester = getLoggedInUser();

        User requestee = findUserByIdentifier(dto.getRecipient().trim());

        if (requester.getId().equals(requestee.getId())) {
            throw new IllegalArgumentException("You cannot request money from yourself");
        }

        MoneyRequest request = new MoneyRequest();
        request.setRequester(requester);
        request.setRequestee(requestee);
        request.setAmount(dto.getAmount());
        request.setPurpose(dto.getPurpose());
        request.setStatus(RequestStatus.PENDING);
        request.setExpiresAt(LocalDateTime.now().plusDays(7));

        moneyRequestRepository.save(request);

        notificationService.sendNotification(
                requestee, NotificationType.MONEY_REQUEST_RECEIVED,
                requester.getFullName() + " has requested ₹" + dto.getAmount() + " from you for: " + dto.getPurpose()
        );

        logger.info("Money request created by {} for ₹{} from {}", requester.getEmail(), dto.getAmount(), requestee.getEmail());

        return MoneyRequestCreateResponse.builder()
                .requestId(request.getRequestId()).amount(request.getAmount())
                .purpose(request.getPurpose()).status(request.getStatus())
                .expiresAt(request.getExpiresAt()).build();
    }

    // ── Find user by email, phone, or username
    private User findUserByIdentifier(String identifier) {

        var byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail.get();

        var byPhone = userRepository.findByPhone(identifier);
        if (byPhone.isPresent()) return byPhone.get();

        var byUsername = userRepository.findByUsername(identifier);
        if (byUsername.isPresent()) return byUsername.get();

        throw new IllegalArgumentException("No RevPay user found with email, phone, or username: " + identifier);
    }

    @Transactional
    public void declineRequest(Long requestId) {

        User user = getLoggedInUser();

        MoneyRequest request = moneyRequestRepository.findByRequestIdAndRequestee(requestId, user)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found or does not belong to this user"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Only pending requests can be declined. Current status: " + request.getStatus());
        }

        request.setStatus(RequestStatus.DECLINED);
        moneyRequestRepository.save(request);

        notificationService.sendNotification(request.getRequester(),
                NotificationType.MONEY_REQUEST_DECLINED,
                user.getFullName() + " declined your money request of ₹" + request.getAmount()
                        + " for " + request.getPurpose()
        );

        logger.info("Money request {} declined by user: {}", requestId, user.getEmail());
    }

    // Incoming requests — only PENDING
    public Page<IncomingRequestResponse> getIncomingRequests(Pageable pageable) {

        User user = getLoggedInUser();

        Page<MoneyRequest> requests = moneyRequestRepository
                .findByRequesteeAndStatusOrderByCreatedAtDesc(
                        user, RequestStatus.PENDING, pageable);

        return requests.map(req -> IncomingRequestResponse.builder()
                .requestId(req.getRequestId())
                .from(IncomingRequestResponse.FromInfo.builder().name(req.getRequester().getFullName())
                .email(req.getRequester().getEmail()).build())
                .amount(req.getAmount()).purpose(req.getPurpose())
                .status(req.getStatus()).createdAt(req.getCreatedAt())
                .expiresAt(req.getExpiresAt()).build());
    }

    // Outgoing requests — all statuses
    public Page<OutgoingRequestResponse> getOutgoingRequests(Pageable pageable) {

        User user = getLoggedInUser();

        Page<MoneyRequest> requests = moneyRequestRepository
                .findByRequesterOrderByCreatedAtDesc(user, pageable);

        return requests.map(req -> OutgoingRequestResponse.builder()
                .requestId(req.getRequestId())
                .to(OutgoingRequestResponse.ToInfo.builder().name(req.getRequestee().getFullName())
                .email(req.getRequestee().getEmail()).build())
                .amount(req.getAmount()).purpose(req.getPurpose()).status(req.getStatus())
                .createdAt(req.getCreatedAt()).build());
    }

    // Accept money request — transfer funds, create transaction record, send notifications
    @Transactional
    public AcceptMoneyRequestResponse acceptRequest(
            Long requestId, AcceptMoneyRequestRequest request) {

        User acceptor = getLoggedInUser();

        if (acceptor.getMtPin() == null) {
            throw new IllegalStateException("Transaction PIN not set. Please set your PIN before making transactions.");
        }

        if (!passwordEncoder.matches(request.getPin(), acceptor.getMtPin())) {
            throw new IllegalArgumentException("Incorrect transaction PIN");
        }

        MoneyRequest moneyRequest = moneyRequestRepository
                .findByRequestIdAndRequestee(requestId, acceptor)
                .orElseThrow(() -> new IllegalArgumentException("Money request not found or does not belong to this user"));

        if (moneyRequest.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending requests can be accepted. Current status: " + moneyRequest.getStatus());
        }

        if (moneyRequest.getExpiresAt() != null && moneyRequest.getExpiresAt().isBefore(LocalDateTime.now())) {
            moneyRequest.setStatus(RequestStatus.EXPIRED);
            moneyRequestRepository.save(moneyRequest);
            throw new IllegalStateException("This money request has expired and can no longer be accepted");
        }

        User requester = moneyRequest.getRequester();
        BigDecimal amount = moneyRequest.getAmount();

        Wallet acceptorWallet = walletRepository.findByUser(acceptor)
                .orElseThrow(() -> new IllegalStateException("Your wallet was not found"));

        if (acceptorWallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    "Insufficient wallet balance. Available: ₹" + acceptorWallet.getBalance() + ", Required: ₹" + amount);
        }

        Wallet requesterWallet = walletRepository.findByUser(requester)
                .orElseThrow(() -> new IllegalStateException("Requester wallet not found"));

        BigDecimal acceptorNewBalance   = acceptorWallet.getBalance().subtract(amount);
        BigDecimal requesterNewBalance  = requesterWallet.getBalance().add(amount);

        acceptorWallet.setBalance(acceptorNewBalance);
        requesterWallet.setBalance(requesterNewBalance);

        walletRepository.save(acceptorWallet);
        walletRepository.save(requesterWallet);

        moneyRequest.setStatus(RequestStatus.ACCEPTED);
        moneyRequestRepository.save(moneyRequest);

        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = new Transaction();
        transaction.setSender(acceptor);
        transaction.setReceiver(requester);
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.SEND);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setNote("Payment for money request: " + moneyRequest.getPurpose());
        transaction.setBalanceAfter(acceptorNewBalance);
        transaction.setCreatedAt(now);
        transactionRepository.save(transaction);

        notificationService.sendNotification(
                acceptor,
                NotificationType.TRANSACTION_SENT,
                "You paid ₹" + amount + " to " + requester.getFullName() + " for: " + moneyRequest.getPurpose()
        );

        notificationService.sendNotification(
                requester,
                NotificationType.TRANSACTION_RECEIVED,
                acceptor.getFullName() + " accepted your money request of ₹" + amount + " for: " + moneyRequest.getPurpose()
        );

        logger.info("Money request {} accepted — ₹{} transferred from {} to {}",
                requestId, amount, acceptor.getEmail(), requester.getEmail());

        return AcceptMoneyRequestResponse.builder()
                .transactionId(transaction.getTransactionId())
                .amount(amount).newBalance(acceptorNewBalance).build();
    }
}
