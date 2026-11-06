package com.alef33.repository;

import com.alef33.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    boolean existsByQuoteId(Long quoteId);
    Optional<CustomerOrder> findTopByNumberStartingWithOrderByNumberDesc(String prefix);
}