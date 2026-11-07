package com.alef33.pdf;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Writes an amount in French words, as required at the bottom of Moroccan invoices:
 * 109800.00 → "Cent neuf mille huit cents dirhams".
 */
public final class AmountInWords {

    private static final String[] UNITS = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
            "dix-sept", "dix-huit", "dix-neuf"
    };
    private static final String[] TENS = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante"
    };

    private AmountInWords() {}

    public static String dirhams(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long dirhams = rounded.longValue();
        int centimes = rounded.subtract(BigDecimal.valueOf(dirhams)).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder(words(dirhams));
        if (dirhams >= 1_000_000 && dirhams % 1_000_000 == 0) {
            sb.append(" de");                       // "un million de dirhams"
        }
        sb.append(dirhams > 1 ? " dirhams" : " dirham");
        if (centimes > 0) {
            sb.append(" et ").append(words(centimes)).append(centimes > 1 ? " centimes" : " centime");
        }
        String s = sb.toString();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String words(long n) {
        if (n == 0) return UNITS[0];
        StringBuilder sb = new StringBuilder();

        long millions = n / 1_000_000;
        long thousands = (n / 1_000) % 1_000;
        int rest = (int) (n % 1_000);

        if (millions > 0) {
            sb.append(belowThousand((int) millions, false)).append(millions > 1 ? " millions" : " million");
        }
        if (thousands > 0) {
            if (!sb.isEmpty()) sb.append(' ');
            if (thousands > 1) sb.append(belowThousand((int) thousands, false)).append(' ');
            sb.append("mille");
        }
        if (rest > 0) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(belowThousand(rest, true));
        }
        return sb.toString();
    }

    private static String belowThousand(int n, boolean isLast) {
        int hundreds = n / 100;
        int rest = n % 100;
        StringBuilder sb = new StringBuilder();
        if (hundreds > 0) {
            if (hundreds > 1) sb.append(UNITS[hundreds]).append(' ');
            sb.append("cent");
            if (hundreds > 1 && rest == 0 && isLast) sb.append('s');
        }
        if (rest > 0) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(belowHundred(rest, isLast));
        }
        return sb.toString();
    }

    private static String belowHundred(int n, boolean isLast) {
        if (n < 20) return UNITS[n];
        int ten = n / 10;
        int unit = n % 10;
        if (ten == 7) {
            return unit == 1 ? "soixante et onze" : "soixante-" + UNITS[10 + unit];
        }
        if (ten == 9) {
            return "quatre-vingt-" + UNITS[10 + unit];
        }
        if (ten == 8) {
            return unit == 0 ? (isLast ? "quatre-vingts" : "quatre-vingt") : "quatre-vingt-" + UNITS[unit];
        }
        String base = TENS[ten];
        if (unit == 0) return base;
        if (unit == 1) return base + " et un";
        return base + "-" + UNITS[unit];
    }
}
