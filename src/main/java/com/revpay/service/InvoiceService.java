package com.revpay.service;

import com.revpay.dto.*;
import com.revpay.enums.*;
import com.revpay.model.*;
import com.revpay.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvoiceService extends BaseService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceLineItemRepository invoiceLineItemRepository;

    @Autowired
    private NotificationService notificationService;

    // List invoices
    public Page<InvoiceListResponse> getInvoices(
            InvoiceStatus status, String search, Pageable pageable) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Page<Invoice> invoices;

        if (search != null && !search.isBlank()) {
            invoices = invoiceRepository.findByUserAndSearch(user, search.trim(), pageable);
        } else if (status != null) {
            invoices = invoiceRepository.findByUserAndStatusOrderByCreatedAtDesc(
                    user, status, pageable);
        } else {
            invoices = invoiceRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        }

        return invoices.map(invoice -> InvoiceListResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(invoice.getCustomerName())
                .customerEmail(invoice.getCustomerEmail())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .dueDate(invoice.getDueDate())
                .createdAt(invoice.getCreatedAt())
                .build());
    }

    // Create invoice
    @Transactional
    public InvoiceCreateResponse createInvoice(CreateInvoiceRequest request) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = new Invoice();
        invoice.setUser(user);
        invoice.setInvoiceNumber(generateInvoiceNumber(user));
        invoice.setCustomerName(request.getCustomer().getName());
        invoice.setCustomerEmail(request.getCustomer().getEmail());
        invoice.setCustomerAddress(request.getCustomer().getAddress());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setPaymentTerms(request.getPaymentTerms() != null
                ? request.getPaymentTerms() : PaymentTerms.NET_30);
        invoice.setNotes(request.getNotes());

        invoice.setDueDate(resolveDueDate(request.getDueDate(), invoice.getPaymentTerms()));

        invoiceRepository.save(invoice);

        BigDecimal totalAmount = buildAndSaveLineItems(invoice, request.getLineItems());
        invoice.setTotalAmount(totalAmount);
        invoiceRepository.save(invoice);

        logger.info("Invoice {} created for user: {}", invoice.getInvoiceNumber(), user.getEmail());

        return InvoiceCreateResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .totalAmount(invoice.getTotalAmount())
                .dueDate(invoice.getDueDate())
                .build();
    }

    // Get invoice detail
    public InvoiceDetailResponse getInvoiceDetail(Long invoiceId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = getInvoiceOrThrow(invoiceId, user);
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoice(invoice);

        return InvoiceDetailResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customer(InvoiceDetailResponse.CustomerInfo.builder()
                        .name(invoice.getCustomerName())
                        .email(invoice.getCustomerEmail())
                        .address(invoice.getCustomerAddress())
                        .build())
                .lineItems(lineItems.stream()
                        .map(item -> {
                            BigDecimal taxAmount = item.getUnitPrice()
                                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                                    .multiply(item.getTaxRate() != null
                                            ? item.getTaxRate() : BigDecimal.ZERO)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                            return InvoiceDetailResponse.LineItemDetail.builder()
                                    .lineItemId(item.getLineItemId())
                                    .description(item.getDescription())
                                    .quantity(item.getQuantity())
                                    .unitPrice(item.getUnitPrice())
                                    .taxRate(item.getTaxRate())
                                    .taxAmount(taxAmount)
                                    .lineTotal(item.getLineTotal())
                                    .build();
                        })
                        .collect(Collectors.toList()))
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .paymentTerms(invoice.getPaymentTerms())
                .dueDate(invoice.getDueDate())
                .notes(invoice.getNotes())
                .sentAt(invoice.getSentAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    // Update invoice
    @Transactional
    public InvoiceUpdateResponse updateInvoice(Long invoiceId, UpdateInvoiceRequest request) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = getInvoiceOrThrow(invoiceId, user);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be edited. Current status: " + invoice.getStatus());
        }

        if (request.getCustomer() != null) {
            if (request.getCustomer().getName() != null) {
                invoice.setCustomerName(request.getCustomer().getName());
            }
            if (request.getCustomer().getEmail() != null) {
                invoice.setCustomerEmail(request.getCustomer().getEmail());
            }
            if (request.getCustomer().getAddress() != null) {
                invoice.setCustomerAddress(request.getCustomer().getAddress());
            }
        }

        if (request.getPaymentTerms() != null) {
            invoice.setPaymentTerms(request.getPaymentTerms());
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        } else if (request.getPaymentTerms() != null) {
            invoice.setDueDate(resolveDueDate(null, invoice.getPaymentTerms()));
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getLineItems() != null && !request.getLineItems().isEmpty()) {
            invoiceLineItemRepository.deleteByInvoice(invoice);

            List<CreateInvoiceRequest.LineItemRequest> mapped = request.getLineItems()
                    .stream()
                    .map(item -> {
                        CreateInvoiceRequest.LineItemRequest r =
                                new CreateInvoiceRequest.LineItemRequest();
                        r.setDescription(item.getDescription());
                        r.setQuantity(item.getQuantity());
                        r.setUnitPrice(item.getUnitPrice());
                        r.setTaxRate(item.getTaxRate());
                        return r;
                    })
                    .collect(Collectors.toList());

            BigDecimal totalAmount = buildAndSaveLineItems(invoice, mapped);
            invoice.setTotalAmount(totalAmount);
        }

        invoiceRepository.save(invoice);

        logger.info("Invoice {} updated for user: {}", invoice.getInvoiceNumber(), user.getEmail());

        return InvoiceUpdateResponse.builder()
                .id(invoice.getId())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .build();
    }

    // Send invoice
    @Transactional
    public InvoiceSentResponse sendInvoice(Long invoiceId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = getInvoiceOrThrow(invoiceId, user);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException(
                    "Only DRAFT invoices can be sent. Current status: " + invoice.getStatus());
        }

        LocalDateTime sentAt = LocalDateTime.now();
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(sentAt);
        invoiceRepository.save(invoice);

        notificationService.sendNotification(
                user,
                NotificationType.INVOICE_SENT,
                "Invoice " + invoice.getInvoiceNumber()
                        + " has been sent to " + invoice.getCustomerEmail()
        );

        logger.info("Invoice {} sent to {} for user: {}",
                invoice.getInvoiceNumber(), invoice.getCustomerEmail(), user.getEmail());

        return InvoiceSentResponse.builder()
                .sentAt(sentAt)
                .build();
    }

    //  Mark as paid
    @Transactional
    public InvoicePaidResponse markInvoicePaid(Long invoiceId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = getInvoiceOrThrow(invoiceId, user);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already marked as paid");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot mark a cancelled invoice as paid");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(paidAt);
        invoiceRepository.save(invoice);

        notificationService.sendNotification(
                user,
                NotificationType.INVOICE_PAID,
                "Invoice " + invoice.getInvoiceNumber()
                        + " of ₹" + invoice.getTotalAmount() + " has been marked as paid"
        );

        logger.info("Invoice {} marked as paid for user: {}",
                invoice.getInvoiceNumber(), user.getEmail());

        return InvoicePaidResponse.builder()
                .paidAt(paidAt)
                .build();
    }

    // Cancel invoice
    @Transactional
    public void cancelInvoice(Long invoiceId) {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        Invoice invoice = getInvoiceOrThrow(invoiceId, user);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already cancelled");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoice);

        logger.info("Invoice {} cancelled for user: {}",
                invoice.getInvoiceNumber(), user.getEmail());
    }

    // Invoice summary
    public InvoiceSummaryResponse getInvoiceSummary() {

        User user = getLoggedInUser();
        assertBusinessAccount(user);

        BigDecimal totalPaid = invoiceRepository
                .sumTotalAmountByUserAndStatus(user, InvoiceStatus.PAID);

        BigDecimal totalUnpaid = invoiceRepository
                .sumTotalAmountByUserAndStatus(user, InvoiceStatus.SENT);

        BigDecimal totalOverdue = invoiceRepository
                .sumTotalAmountByUserAndStatus(user, InvoiceStatus.OVERDUE);

        long paidCount    = invoiceRepository.countByUserAndStatus(user, InvoiceStatus.PAID);
        long unpaidCount  = invoiceRepository.countByUserAndStatus(user, InvoiceStatus.SENT);
        long overdueCount = invoiceRepository.countByUserAndStatus(user, InvoiceStatus.OVERDUE);

        return InvoiceSummaryResponse.builder()
                .totalPaid(totalPaid)
                .totalUnpaid(totalUnpaid)
                .totalOverdue(totalOverdue)
                .invoiceCount(InvoiceSummaryResponse.InvoiceCount.builder()
                        .paid(paidCount)
                        .unpaid(unpaidCount)
                        .overdue(overdueCount)
                        .build())
                .build();
    }


    private Invoice getInvoiceOrThrow(Long invoiceId, User user) {
        return invoiceRepository.findByIdAndUser(invoiceId, user)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invoice not found or does not belong to this account"));
    }

    private BigDecimal buildAndSaveLineItems(Invoice invoice,
            List<CreateInvoiceRequest.LineItemRequest> lineItemRequests) {

        BigDecimal total = BigDecimal.ZERO;

        for (CreateInvoiceRequest.LineItemRequest item : lineItemRequests) {
            BigDecimal taxRate = item.getTaxRate() != null
                    ? item.getTaxRate() : BigDecimal.ZERO;

            BigDecimal subtotal = item.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal taxAmount = subtotal
                    .multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal lineTotal = subtotal.add(taxAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setInvoice(invoice);
            lineItem.setDescription(item.getDescription());
            lineItem.setQuantity(item.getQuantity());
            lineItem.setUnitPrice(item.getUnitPrice());
            lineItem.setTaxRate(taxRate);
            lineItem.setLineTotal(lineTotal);
            invoiceLineItemRepository.save(lineItem);

            total = total.add(lineTotal);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate resolveDueDate(LocalDate explicitDate, PaymentTerms terms) {
        if (explicitDate != null) return explicitDate;

        int days = switch (terms != null ? terms : PaymentTerms.NET_30) {
            case NET_7  -> 7;
            case NET_15 -> 15;
            case NET_30 -> 30;
            case NET_60 -> 60;
        };

        return LocalDate.now().plusDays(days);
    }

    private String generateInvoiceNumber(User user) {
        long count = invoiceRepository.countByUser(user);
        return "INV-" + user.getId() + "-" + String.format("%04d", count + 1);
    }

    private void assertBusinessAccount(User user) {
        if (user.getAccountType() != AccountType.BUSINESS) {
            throw new IllegalStateException(
                    "Invoices are only available for business accounts");
        }
    }
}