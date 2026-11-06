package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.DocumentLine;
import com.alef33.model.SalesDocument;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** quantity × unit price HT, rounded to 2 decimals */
    public BigDecimal lineTotalHT(DocumentLine line) {
        return line.getUnitPriceHT()
                .multiply(BigDecimal.valueOf(line.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Computes each line total, then the document's HT, VAT and TTC */
    public void computeTotals(SalesDocument document, List<? extends DocumentLine> lines) {
        if (lines.isEmpty()) {
            throw new BusinessException("A document must have at least one line");
        }

        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalVat = BigDecimal.ZERO;

        for (DocumentLine line : lines) {
            if (line.getQuantity() <= 0) {
                throw new BusinessException("Quantity must be greater than 0");
            }
            if (line.getUnitPriceHT() == null || line.getUnitPriceHT().signum() < 0) {
                throw new BusinessException("Unit price must be 0 or more");
            }

            BigDecimal lineHT = lineTotalHT(line);
            line.setLineTotalHT(lineHT);

            BigDecimal lineVat = lineHT.multiply(line.getVatRate())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);

            totalHT = totalHT.add(lineHT);
            totalVat = totalVat.add(lineVat);
        }

        document.setTotalHT(totalHT);
        document.setTotalVat(totalVat);
        document.setTotalTTC(totalHT.add(totalVat));
    }
}