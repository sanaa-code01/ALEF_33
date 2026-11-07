package com.alef33.service;

import com.alef33.dto.ProductRequest;
import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.Product;
import com.alef33.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final BigDecimal DEFAULT_VAT = new BigDecimal("20.00");

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> list(boolean activeOnly) {
        return activeOnly
                ? productRepository.findByActiveTrueOrderByDesignationAsc()
                : productRepository.findAllByOrderByDesignationAsc();
    }

    @Transactional(readOnly = true)
    public Product get(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        if (productRepository.existsByReference(request.reference())) {
            throw new BusinessException("Reference already used: " + request.reference());
        }
        Product product = new Product();
        apply(product, request);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = get(id);
        if (productRepository.existsByReferenceAndIdNot(request.reference(), id)) {
            throw new BusinessException("Reference already used: " + request.reference());
        }
        apply(product, request);
        return product;
    }

    /**
     * Products are never really deleted: old quotes and invoices point to them.
     * "Delete" only deactivates the product, so it can't be used in new quotes.
     */
    @Transactional
    public void deactivate(Long id) {
        get(id).setActive(false);
    }

    private void apply(Product product, ProductRequest r) {
        BigDecimal vat = r.vatRate() != null ? r.vatRate() : DEFAULT_VAT;
        product.setReference(r.reference().trim());
        product.setDesignation(r.designation().trim());
        product.setDescription(r.description());
        product.setUnitPriceTTC(r.unitPriceTTC());
        product.setVatRate(vat);
        product.setUnitPriceHT(PricingService.htFromTtc(r.unitPriceTTC(), vat));
        product.setActive(r.active() == null || r.active());
    }
}
