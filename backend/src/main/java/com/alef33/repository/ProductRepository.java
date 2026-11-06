package com.alef33.repository;

import com.alef33.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByReference(String reference);

    boolean existsByReferenceAndIdNot(String reference, Long id);

    List<Product> findAllByOrderByDesignationAsc();

    List<Product> findByActiveTrueOrderByDesignationAsc();
}