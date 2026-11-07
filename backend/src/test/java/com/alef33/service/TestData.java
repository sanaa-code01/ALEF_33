package com.alef33.service;

import com.alef33.model.Customer;
import com.alef33.model.DocumentLine;
import com.alef33.model.QuoteLine;

import java.math.BigDecimal;

/** Small helpers to build test objects */
final class TestData {

    static final BigDecimal VAT_20 = new BigDecimal("20.00");

    private TestData() {}

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    static Customer customer() {
        Customer c = new Customer();
        c.setId(1L);
        c.setName("Mr. Ali");
        c.setCity("Agadir");
        return c;
    }

    /** A line with a TTC unit price, like ALEF 33 types them */
    static <T extends DocumentLine> T line(T line, String designation, int quantity, String unitPriceTTC) {
        line.setDesignation(designation);
        line.setQuantity(quantity);
        line.setUnitPriceTTC(money(unitPriceTTC));
        line.setVatRate(VAT_20);
        return line;
    }

    static QuoteLine quoteLine(String designation, int quantity, String unitPriceTTC) {
        return line(new QuoteLine(), designation, quantity, unitPriceTTC);
    }
}
