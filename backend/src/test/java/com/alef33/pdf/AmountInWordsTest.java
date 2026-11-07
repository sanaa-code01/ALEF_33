package com.alef33.pdf;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Amount in words (bottom of quotes and invoices)")
class AmountInWordsTest {

    private static String words(String amount) {
        return AmountInWords.dirhams(new BigDecimal(amount));
    }

    @Test
    @DisplayName("Totals of real ALEF 33 documents")
    void realDocumentTotals() {
        assertThat(words("95800.00")).isEqualTo("Quatre-vingt-quinze mille huit cents dirhams");
        assertThat(words("68520.00")).isEqualTo("Soixante-huit mille cinq cent vingt dirhams");
        assertThat(words("109800.00")).isEqualTo("Cent neuf mille huit cents dirhams");
    }

    @Test
    @DisplayName("French rules: et un, soixante et onze, quatre-vingts, cents")
    void frenchSpellingRules() {
        assertThat(words("21")).isEqualTo("Vingt et un dirhams");
        assertThat(words("71")).isEqualTo("Soixante et onze dirhams");
        assertThat(words("80")).isEqualTo("Quatre-vingts dirhams");
        assertThat(words("81")).isEqualTo("Quatre-vingt-un dirhams");
        assertThat(words("99")).isEqualTo("Quatre-vingt-dix-neuf dirhams");
        assertThat(words("200")).isEqualTo("Deux cents dirhams");
        assertThat(words("201")).isEqualTo("Deux cent un dirhams");
        assertThat(words("180000")).isEqualTo("Cent quatre-vingt mille dirhams");
    }

    @Test
    @DisplayName("Singular, thousands, millions and centimes")
    void specialCases() {
        assertThat(words("1")).isEqualTo("Un dirham");
        assertThat(words("1000")).isEqualTo("Mille dirhams");
        assertThat(words("1000000")).isEqualTo("Un million de dirhams");
        assertThat(words("2300000")).isEqualTo("Deux millions trois cent mille dirhams");
        assertThat(words("1001.01")).isEqualTo("Mille un dirhams et un centime");
        assertThat(words("2.50")).isEqualTo("Deux dirhams et cinquante centimes");
    }
}
