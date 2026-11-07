package com.alef33.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Year;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Document numbering")
class DocumentNumberServiceTest {

    private final DocumentNumberService numbers = new DocumentNumberService();

    @Test
    @DisplayName("Prefix contains the type and the current year")
    void yearPrefix() {
        assertThat(numbers.yearPrefix("FAC")).isEqualTo("FAC-" + Year.now().getValue() + "-");
    }

    @Test
    @DisplayName("First document of the year is 0001")
    void firstOfTheYear() {
        assertThat(numbers.next("DEV-2026-", Optional.empty())).isEqualTo("DEV-2026-0001");
    }

    @Test
    @DisplayName("Next number follows the last one")
    void followsTheLast() {
        assertThat(numbers.next("DEV-2026-", Optional.of("DEV-2026-0069"))).isEqualTo("DEV-2026-0070");
    }
}
