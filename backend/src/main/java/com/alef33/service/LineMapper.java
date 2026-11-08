package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.DocumentLine;
import com.alef33.model.Product;
import com.alef33.repository.ProductRepository;
import com.alef33.service.dto.LineInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class LineMapper {

    private static final BigDecimal DEFAULT_VAT = new BigDecimal("20.00");

    private final ProductRepository productRepository;

    /** Fills a new line from user input (with product defaults) */
    public <T extends DocumentLine> T fromInput(T line, LineInput input) {
        Product product = null;
        if (input.productId() != null) {
            product = productRepository.findById(input.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", input.productId()));
            if (!product.isActive()) {
                throw new BusinessException("Le produit " + product.getReference() + " n'est plus actif");
            }
        }

        line.setProduct(product);
        line.setDesignation(firstNonNull(input.designation(),
                product != null ? product.getDesignation() : null));
        line.setQuantity(input.quantity());
        line.setUnitPriceTTC(firstNonNull(input.unitPriceTTC(),
                product != null ? product.getUnitPriceTTC() : null));
        line.setVatRate(firstNonNull(input.vatRate(),
                product != null ? product.getVatRate() : DEFAULT_VAT));

        if (line.getDesignation() == null || line.getDesignation().isBlank()) {
            throw new BusinessException("Chaque ligne doit avoir une désignation ou un produit");
        }
        if (line.getUnitPriceTTC() == null) {
            throw new BusinessException("Chaque ligne doit avoir un prix TTC ou un produit");
        }
        return line;
    }

    /** Copies a line to another document (quote → order → invoice) */
    public <T extends DocumentLine> T copy(DocumentLine source, T target) {
        target.setProduct(source.getProduct());
        target.setDesignation(source.getDesignation());
        target.setQuantity(source.getQuantity());
        target.setUnitPriceTTC(source.getUnitPriceTTC());
        target.setUnitPriceHT(source.getUnitPriceHT());
        target.setVatRate(source.getVatRate());
        target.setLineTotalTTC(source.getLineTotalTTC());
        target.setLineTotalHT(source.getLineTotalHT());
        return target;
    }

    private static <V> V firstNonNull(V a, V b) {
        return a != null ? a : b;
    }
}
