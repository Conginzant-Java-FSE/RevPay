package com.revpay.repository;

import com.revpay.enums.RequestStatus;
import com.revpay.model.MoneyRequest;
import com.revpay.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {

    // Incoming requests — requests sent TO this user (they need to accept/decline)
    Page<MoneyRequest> findByRequesteeAndStatusOrderByCreatedAtDesc(
            User requestee, RequestStatus status, Pageable pageable);

    // Outgoing requests — requests sent BY this user
    Page<MoneyRequest> findByRequesterOrderByCreatedAtDesc(
            User requester, Pageable pageable);

    // Find specific request — used when accepting/declining/cancelling
    Optional<MoneyRequest> findByRequestIdAndRequestee(Long requestId, User requestee);
    Optional<MoneyRequest> findByRequestIdAndRequester(Long requestId, User requester);
}