package com.revpay.repository;

import com.revpay.model.Invoice;
import com.revpay.model.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    List<InvoiceLineItem> findByInvoice(Invoice invoice);

    void deleteByInvoice(Invoice invoice);
}