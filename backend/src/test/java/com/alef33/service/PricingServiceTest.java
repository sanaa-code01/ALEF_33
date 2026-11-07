package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.Quote;
import com.alef33.model.QuoteLine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.alef33.service.TestData.money;
import static com.alef33.service.TestData.quoteLine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pricing: TTC → HT → VAT")
class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    @Test
    @DisplayName("4 shelves at 800 TTC give exactly 3 200,00 TTC (no lost centimes)")
    void fourShelvesAt800() {
        Quote quote = new Quote();
        QuoteLine shelves = quoteLine("Étagère Inox 80 / 33cm", 4, "800.00");

        pricing.computeTotals(quote, List.of(shelves));

        assertThat(shelves.getLineTotalTTC()).isEqualByComparingTo("3200.00");
        assertThat(shelves.getLineTotalHT()).isEqualByComparingTo("2666.67");
        assertThat(shelves.getUnitPriceHT()).isEqualByComparingTo("666.67");
        assertThat(quote.getTotalTTC()).isEqualByComparingTo("3200.00");
        assertThat(quote.getTotalVat()).isEqualByComparingTo("533.33");
    }

    @Test
    @DisplayName("Mr. Ali's real quote: 11 lines = exactly 95 800,00 TTC, and HT + VAT = TTC")
    void mrAliQuote() {
        Quote quote = new Quote();
        List<QuoteLine> lines = List.of(
                quoteLine("Habillage de façade", 1, "25000"),
                quoteLine("Hotte Inox avec filtres", 1, "9500"),
                quoteLine("Chawaya Inox", 1, "8000"),
                quoteLine("Comptoir Réfrigéré Inox 3 portes", 1, "10000"),
                quoteLine("Plonge avec étagère", 1, "6500"),
                quoteLine("Comptoir Décoré", 1, "12000"),
                quoteLine("Friteuse double à gaz", 1, "6000"),
                quoteLine("Caisson 12/12 extracteur", 1, "9000"),
                quoteLine("12m gaine 30cm", 1, "3600"),
                quoteLine("Table de travail Inox", 1, "3000"),
                quoteLine("Étagère Inox 80 / 33cm", 4, "800"));

        pricing.computeTotals(quote, lines);

        assertThat(quote.getTotalTTC()).isEqualByComparingTo("95800.00");
        assertThat(quote.getTotalHT().add(quote.getTotalVat())).isEqualByComparingTo(quote.getTotalTTC());
    }

    @Test
    @DisplayName("HT is derived from TTC: 54 000 TTC at 20 % = 45 000 HT")
    void htFromTtc() {
        assertThat(PricingService.htFromTtc(money("54000.00"), money("20.00"))).isEqualByComparingTo("45000.00");
        assertThat(PricingService.htFromTtc(money("1100.00"), money("10.00"))).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Lines with different VAT rates on the same quote")
    void mixedVatRates() {
        Quote quote = new Quote();
        QuoteLine at20 = quoteLine("Four", 1, "1200.00");
        QuoteLine at10 = quoteLine("Pièce détachée", 1, "1100.00");
        at10.setVatRate(money("10.00"));

        pricing.computeTotals(quote, List.of(at20, at10));

        assertThat(quote.getTotalHT()).isEqualByComparingTo("2000.00");   // 1000 + 1000
        assertThat(quote.getTotalVat()).isEqualByComparingTo("300.00");   // 200 + 100
        assertThat(quote.getTotalTTC()).isEqualByComparingTo("2300.00");
    }

    @Test
    @DisplayName("A document without lines is refused")
    void noLines() {
        assertThatThrownBy(() -> pricing.computeTotals(new Quote(), List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("at least one line");
    }

    @Test
    @DisplayName("Quantity 0 and negative prices are refused")
    void invalidLines() {
        assertThatThrownBy(() -> pricing.computeTotals(new Quote(), List.of(quoteLine("Four", 0, "100"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Quantity");

        assertThatThrownBy(() -> pricing.computeTotals(new Quote(), List.of(quoteLine("Four", 1, "-5"))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("price");
    }
}
