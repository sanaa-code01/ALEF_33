package com.alef33.repository;

import com.alef33.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByOrderId(Long orderId);
    Optional<Invoice> findTopByNumberStartingWithOrderByNumberDesc(String prefix);

    Optional<Invoice> findByOrderId(Long orderId);
}