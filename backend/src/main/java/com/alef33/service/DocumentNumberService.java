package com.alef33.service;

import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.Optional;

@Service
public class DocumentNumberService {

    /** e.g. "DEV" → "DEV-2026-" */
    public String yearPrefix(String type) {
        return type + "-" + Year.now().getValue() + "-";
    }

    /** e.g. last = "DEV-2026-0007" → "DEV-2026-0008"; no last → "DEV-2026-0001" */
    public String next(String yearPrefix, Optional<String> lastNumber) {
        int next = lastNumber
                .map(n -> Integer.parseInt(n.substring(yearPrefix.length())) + 1)
                .orElse(1);
        return yearPrefix + String.format("%04d", next);
    }
}