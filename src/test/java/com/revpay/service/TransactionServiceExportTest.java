package com.revpay.service;

import com.revpay.model.Transaction;
import com.revpay.enums.TransactionStatus;
import com.revpay.enums.TransactionType;
import com.revpay.model.User;
import com.revpay.repository.TransactionRepository;
import com.revpay.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceExportTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    public void setup() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        when(authentication.getName()).thenReturn("test@example.com");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    public void testExportCsv() throws Exception {
        Transaction t1 = new Transaction();
        t1.setTransactionId(12345L);
        t1.setAmount(new BigDecimal("100.00"));
        t1.setCurrency("USD");
        t1.setTransactionType(TransactionType.SEND);
        t1.setStatus(TransactionStatus.SUCCESS);
        t1.setCreatedAt(LocalDateTime.now());
        t1.setUpdatedAt(LocalDateTime.now());
        t1.setBalanceAfter(new BigDecimal("1000.00"));

        List<Transaction> mockTransactions = Arrays.asList(t1);

        when(transactionRepository.findAllByUserWithFilters(any(), any(), any(), any(), any(), any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockTransactions));

        MockHttpServletResponse response = new MockHttpServletResponse();

        transactionService.exportTransactions("CSV", null, null, null, null, null, response);

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0);

        String contentString = new String(content);
        assertTrue(contentString.contains("12345"));
        assertTrue(contentString.contains("100.00"));
        assertTrue(response.getContentType().equals("text/csv"));
        assertTrue(response.getHeader("Content-Disposition").contains("revpay-transactions.csv"));
    }

    @Test
    public void testExportPdf() throws Exception {
        Transaction t1 = new Transaction();
        t1.setTransactionId(67890L);
        t1.setAmount(new BigDecimal("200.00"));
        t1.setCurrency("EUR");
        t1.setTransactionType(TransactionType.ADD_MONEY);
        t1.setStatus(TransactionStatus.SUCCESS);
        t1.setCreatedAt(LocalDateTime.now());
        t1.setUpdatedAt(LocalDateTime.now());
        t1.setBalanceAfter(new BigDecimal("1500.00"));

        List<Transaction> mockTransactions = Arrays.asList(t1);

        when(transactionRepository.findAllByUserWithFilters(any(), any(), any(), any(), any(), any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(mockTransactions));

        MockHttpServletResponse response = new MockHttpServletResponse();

        transactionService.exportTransactions("PDF", null, null, null, null, null, response);

        byte[] content = response.getContentAsByteArray();
        assertTrue(content.length > 0);
        assertTrue(content[0] == 0x25); // %
        assertTrue(content[1] == 0x50); // P
        assertTrue(content[2] == 0x44); // D
        assertTrue(content[3] == 0x46); // F
        assertTrue(response.getContentType().equals("application/pdf"));
        assertTrue(response.getHeader("Content-Disposition").contains("revpay-transactions.pdf"));
    }
}
