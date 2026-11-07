package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.DocumentLine;
import com.alef33.model.SalesDocument;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Totals are calculated from TTC prices, because that's how ALEF 33 sells:
 * the TTC promised to the customer never changes, HT and VAT are derived from it.
 *
 *   line TTC = unit price TTC × quantity        (exact)
 *   line HT  = line TTC ÷ (1 + rate / 100)      (rounded to 2 decimals)
 *   line VAT = line TTC − line HT               (absorbs the rounding)
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** e.g. 3200.00 TTC at 20 % → 2666.67 HT */
    public static BigDecimal htFromTtc(BigDecimal amountTTC, BigDecimal vatRate) {
        BigDecimal divisor = BigDecimal.ONE.add(vatRate.divide(HUNDRED, 6, RoundingMode.HALF_UP));
        return amountTTC.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /** quantity × unit price TTC */
    public BigDecimal lineTotalTTC(DocumentLine line) {
        return line.getUnitPriceTTC()
                .multiply(BigDecimal.valueOf(line.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Computes each line, then the document's TTC, HT and VAT */
    public void computeTotals(SalesDocument document, List<? extends DocumentLine> lines) {
        if (lines.isEmpty()) {
            throw new BusinessException("A document must have at least one line");
        }

        BigDecimal totalTTC = BigDecimal.ZERO;
        BigDecimal totalHT = BigDecimal.ZERO;

        for (DocumentLine line : lines) {
            if (line.getQuantity() <= 0) {
                throw new BusinessException("Quantity must be greater than 0");
            }
            if (line.getUnitPriceTTC() == null || line.getUnitPriceTTC().signum() < 0) {
                throw new BusinessException("Unit price must be 0 or more");
            }

            BigDecimal lineTTC = lineTotalTTC(line);
            BigDecimal lineHT = htFromTtc(lineTTC, line.getVatRate());

            line.setUnitPriceHT(htFromTtc(line.getUnitPriceTTC(), line.getVatRate()));
            line.setLineTotalTTC(lineTTC);
            line.setLineTotalHT(lineHT);

            totalTTC = totalTTC.add(lineTTC);
            totalHT = totalHT.add(lineHT);
        }

        document.setTotalTTC(totalTTC);
        document.setTotalHT(totalHT);
        document.setTotalVat(totalTTC.subtract(totalHT));
    }
}
