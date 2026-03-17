package com.revpay.service;

import com.revpay.dto.CreateSplitRequest;
import com.revpay.dto.SplitPaymentResponse;
import com.revpay.enums.NotificationType;
import com.revpay.model.*;
import com.revpay.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SplitPaymentService {

    @Autowired
    private SplitPaymentRepository splitPaymentRepository;

    @Autowired
    private SplitPaymentParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private NotificationService notificationService;

    private User getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public SplitPaymentResponse createSplit(CreateSplitRequest request) {
        User creator = getLoggedInUser();

        // Check PIN
        if (creator.getMtPin() == null || !passwordEncoder.matches(request.getPin(), creator.getMtPin())) {
            throw new RuntimeException("Incorrect transaction PIN");
        }

        SplitPayment split = new SplitPayment();
        split.setCreatedByUser(creator);
        split.setTotalAmount(request.getTotalAmount());
        split.setNote(request.getNote());
        split = splitPaymentRepository.save(split);

        for (CreateSplitRequest.SplitParticipantDTO pDto : request.getParticipants()) {
            User participant = userRepository.findByEmail(pDto.getEmailOrPhone())
                    .or(() -> userRepository.findByPhone(pDto.getEmailOrPhone()))
                    .orElseThrow(() -> new RuntimeException("User not found: " + pDto.getEmailOrPhone()));

            if (participant.getId().equals(creator.getId())) continue;

            SplitPaymentParticipant participantEntity = new SplitPaymentParticipant();
            participantEntity.setSplit(split);
            participantEntity.setUser(participant);
            participantEntity.setAmountOwed(pDto.getAmount());
            participantRepository.save(participantEntity);

            notificationService.sendNotification(participant, NotificationType.GENERAL,
                    creator.getFullName() + " tagged you in a split payment of ₹" + pDto.getAmount());
        }

        return mapToResponse(split);
    }

    @Transactional
    public List<SplitPaymentResponse> getMySplits() {
        return splitPaymentRepository.findByCreatedByUserOrderByCreatedAtDesc(getLoggedInUser())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public List<SplitPaymentResponse> getOwedSplits() {
        return participantRepository.findByUserAndPaidFalse(getLoggedInUser())
                .stream().map(p -> mapToResponse(p.getSplit())).collect(Collectors.toList());
    }

    @Transactional
    public void payShare(Long splitId, String pin) {
        User participant = getLoggedInUser();

        // Check PIN
        if (participant.getMtPin() == null || !passwordEncoder.matches(pin, participant.getMtPin())) {
            throw new RuntimeException("Incorrect transaction PIN");
        }

        SplitPaymentParticipant pEntity = participantRepository.findBySplit(splitPaymentRepository.findById(splitId).orElseThrow())
                .stream().filter(p -> p.getUser().getId().equals(participant.getId())).findFirst()
                .orElseThrow(() -> new RuntimeException("You are not part of this split"));

        if (pEntity.isPaid()) throw new RuntimeException("Already paid");

        Wallet pWallet = walletRepository.findByUser(participant).orElseThrow();
        if (pWallet.getBalance().compareTo(pEntity.getAmountOwed()) < 0) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        User creator = pEntity.getSplit().getCreatedByUser();
        Wallet cWallet = walletRepository.findByUser(creator).orElseThrow();

        // Deduct from participant
        pWallet.setBalance(pWallet.getBalance().subtract(pEntity.getAmountOwed()));
        walletRepository.save(pWallet);

        // Credit to creator
        cWallet.setBalance(cWallet.getBalance().add(pEntity.getAmountOwed()));
        walletRepository.save(cWallet);

        // Create transaction records
        Transaction t = new Transaction();
        t.setSender(participant);
        t.setReceiver(creator);
        t.setAmount(pEntity.getAmountOwed());
        t.setCurrency("INR");
        t.setTransactionType(com.revpay.enums.TransactionType.SEND);
        t.setStatus(com.revpay.enums.TransactionStatus.SUCCESS);
        t.setUtrNumber("SPLIT-" + UUID.randomUUID().toString().substring(0, 8));
        t.setNote("Settled share for split: " + pEntity.getSplit().getNote());
        transactionRepository.save(t);

        // Mark as paid
        pEntity.setPaid(true);
        pEntity.setPaidAt(LocalDateTime.now());
        participantRepository.save(pEntity);

        // Check if all settled
        List<SplitPaymentParticipant> all = participantRepository.findBySplit(pEntity.getSplit());
        boolean allPaid = all.stream().allMatch(SplitPaymentParticipant::isPaid);
        if (allPaid) {
            SplitPayment s = pEntity.getSplit();
            s.setStatus("SETTLED");
            splitPaymentRepository.save(s);
        }

        notificationService.sendNotification(creator, NotificationType.TRANSACTION_RECEIVED,
                participant.getFullName() + " paid their share of ₹" + pEntity.getAmountOwed());
    }

    private SplitPaymentResponse mapToResponse(SplitPayment s) {
        SplitPaymentResponse res = new SplitPaymentResponse();
        res.setSplitId(s.getId());
        res.setCreatedByName(s.getCreatedByUser().getFullName());
        res.setTotalAmount(s.getTotalAmount());
        res.setNote(s.getNote());
        res.setStatus(s.getStatus());
        res.setCreatedAt(s.getCreatedAt());
        res.setParticipants(participantRepository.findBySplit(s).stream().map(p -> {
            SplitPaymentResponse.ParticipantResponse pr = new SplitPaymentResponse.ParticipantResponse();
            pr.setFullName(p.getUser().getFullName());
            pr.setEmailOrPhone(p.getUser().getEmail());
            pr.setAmountOwed(p.getAmountOwed());
            pr.setPaid(p.isPaid());
            pr.setPaidAt(p.getPaidAt());
            return pr;
        }).collect(Collectors.toList()));
        return res;
    }
}
