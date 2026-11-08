package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.*;
import com.alef33.model.enums.QuoteStatus;
import com.alef33.repository.CustomerRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.QuoteRepository;
import com.alef33.service.dto.LineInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final PricingService pricingService;
    private final DocumentNumberService numberService;
    private final LineMapper lineMapper;

    @Transactional(readOnly = true)
    public Quote get(Long id) {
        return quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", id));
    }

    @Transactional
    public Quote create(Long customerId, List<LineInput> lines, LocalDate validUntil, String notes) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));

        Quote quote = new Quote();
        quote.setCustomer(customer);
        quote.setValidUntil(validUntil != null ? validUntil : LocalDate.now().plusDays(30));
        quote.setNotes(notes);

        for (LineInput input : lines) {
            quote.addLine(lineMapper.fromInput(new QuoteLine(), input));
        }
        pricingService.computeTotals(quote, quote.getLines());

        String prefix = numberService.yearPrefix("DEV");
        quote.setNumber(numberService.next(prefix,
                quoteRepository.findTopByNumberStartingWithOrderByNumberDesc(prefix).map(Quote::getNumber)));

        return quoteRepository.save(quote);
    }

    @Transactional
    public Quote send(Long id) {
        Quote quote = get(id);
        requireStatus(quote, QuoteStatus.DRAFT);
        quote.setStatus(QuoteStatus.SENT);
        return quote;
    }

    @Transactional
    public Quote accept(Long id) {
        Quote quote = get(id);
        requireStatus(quote, QuoteStatus.DRAFT, QuoteStatus.SENT);
        if (quote.getValidUntil() != null && quote.getValidUntil().isBefore(LocalDate.now())) {
            throw new BusinessException("Le devis " + quote.getNumber() + " a expiré");
        }
        quote.setStatus(QuoteStatus.ACCEPTED);
        return quote;
    }

    @Transactional
    public Quote reject(Long id) {
        Quote quote = get(id);
        requireStatus(quote, QuoteStatus.DRAFT, QuoteStatus.SENT);
        quote.setStatus(QuoteStatus.REJECTED);
        return quote;
    }

    @Transactional
    public CustomerOrder convertToOrder(Long quoteId) {
        Quote quote = get(quoteId);
        requireStatus(quote, QuoteStatus.ACCEPTED);
        if (orderRepository.existsByQuoteId(quoteId)) {
            throw new BusinessException("Le devis " + quote.getNumber() + " a déjà été transformé en commande");
        }

        CustomerOrder order = new CustomerOrder();
        order.setQuote(quote);
        order.setCustomer(quote.getCustomer());
        order.setNotes(quote.getNotes());
        for (QuoteLine line : quote.getLines()) {
            order.addLine(lineMapper.copy(line, new OrderLine()));
        }
        pricingService.computeTotals(order, order.getLines());

        String prefix = numberService.yearPrefix("CMD");
        order.setNumber(numberService.next(prefix,
                orderRepository.findTopByNumberStartingWithOrderByNumberDesc(prefix).map(CustomerOrder::getNumber)));

        quote.setStatus(QuoteStatus.CONVERTED);
        return orderRepository.save(order);
    }

    private void requireStatus(Quote quote, QuoteStatus... allowed) {
        for (QuoteStatus s : allowed) {
            if (quote.getStatus() == s) return;
        }
        throw new BusinessException("Le devis " + quote.getNumber() + " est « " + label(quote.getStatus())
                + " » : action impossible");
    }

    private static String label(QuoteStatus status) {
        return switch (status) {
            case DRAFT -> "Brouillon";
            case SENT -> "Envoyé";
            case ACCEPTED -> "Accepté";
            case REJECTED -> "Refusé";
            case CONVERTED -> "Transformé en commande";
        };
    }
}
