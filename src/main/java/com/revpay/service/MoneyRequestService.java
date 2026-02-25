package com.revpay.service;

import com.revpay.dto.MoneyRequestCreateRequest;
import com.revpay.enums.RequestStatus;
import com.revpay.model.MoneyRequest;
import com.revpay.model.User;
import com.revpay.repository.MoneyRequestRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MoneyRequestService {

    private final MoneyRequestRepository moneyRequestRepository;
    private final UserRepository userRepository;

    public MoneyRequestService(MoneyRequestRepository moneyRequestRepository,
                               UserRepository userRepository) {
        this.moneyRequestRepository = moneyRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void cancelRequest(Long requestId, String email) {

        // 1️⃣ Get logged-in user
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2️⃣ Find request ONLY if it belongs to this requester
        MoneyRequest request = moneyRequestRepository
                .findByRequestIdAndRequester(requestId, requester)
                .orElseThrow(() -> new RuntimeException("Request not found or not yours"));

        // 3️⃣ Allow cancel only if PENDING
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("Only pending requests can be cancelled");
        }

        // 4️⃣ Update status
        request.setStatus(RequestStatus.CANCELLED);

        moneyRequestRepository.save(request);
    }
    @Transactional
    public MoneyRequest createRequest(MoneyRequestCreateRequest dto, String email) {

        // 1️⃣ Get requester (logged in user)
        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2️⃣ Find recipient (for now assume recipient is email)
        User requestee = userRepository.findByEmail(dto.getRecipient())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        // Prevent self request
        if (requester.getId().equals(requestee.getId())) {
            throw new RuntimeException("Cannot request money from yourself");
        }

        // 3️⃣ Create entity
        MoneyRequest request = new MoneyRequest();
        request.setRequester(requester);
        request.setRequestee(requestee);
        request.setAmount(dto.getAmount());
        request.setPurpose(dto.getPurpose());
        request.setStatus(RequestStatus.PENDING);

        // 4️⃣ Expiry = 7 days from now
        request.setExpiresAt(LocalDateTime.now().plusDays(7));

        return moneyRequestRepository.save(request);
    }
}
