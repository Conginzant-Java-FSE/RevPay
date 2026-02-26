package com.revpay.service;

import com.revpay.dto.MoneyRequestCreateRequest;
import com.revpay.enums.NotificationType;
import com.revpay.enums.RecordStatus;
import com.revpay.enums.RequestStatus;
import com.revpay.model.MoneyRequest;
import com.revpay.model.Notification;
import com.revpay.model.User;
import com.revpay.repository.MoneyRequestRepository;
import com.revpay.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
    public MoneyRequest createRequest(MoneyRequestCreateRequest dto, String email) {

        User requester = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User requestee = userRepository.findByEmail(dto.getRecipient())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (requester.getId().equals(requestee.getId())) {
            throw new RuntimeException("Cannot request money from yourself");
        }

        MoneyRequest request = new MoneyRequest();
        request.setRequester(requester);
        request.setRequestee(requestee);
        request.setAmount(dto.getAmount());
        request.setPurpose(dto.getPurpose());
        request.setStatus(RequestStatus.PENDING);


        request.setExpiresAt(LocalDateTime.now().plusDays(7));

        return moneyRequestRepository.save(request);
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
}
