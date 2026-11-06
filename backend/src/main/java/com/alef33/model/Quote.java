package com.alef33.model;

import com.alef33.model.enums.QuoteStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
public class Quote extends SalesDocument {

    @Column(nullable = false)
    private LocalDate issueDate = LocalDate.now();

    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuoteStatus status = QuoteStatus.DRAFT;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteLine> lines = new ArrayList<>();

    public void addLine(QuoteLine line) {
        lines.add(line);
        line.setQuote(this);
    }

    public void removeLine(QuoteLine line) {
        lines.remove(line);
        line.setQuote(null);
    }
}