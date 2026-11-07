package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Product;
import com.alef33.model.Quote;
import com.alef33.model.enums.QuoteStatus;
import com.alef33.repository.CustomerRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.ProductRepository;
import com.alef33.repository.QuoteRepository;
import com.alef33.service.dto.LineInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.alef33.service.TestData.customer;
import static com.alef33.service.TestData.money;
import static com.alef33.service.TestData.quoteLine;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Quotes: creation, status rules and conversion to an order")
class QuoteServiceTest {

    @Mock QuoteRepository quoteRepository;
    @Mock OrderRepository orderRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;

    private final PricingService pricing = new PricingService();
    private final DocumentNumberService numbers = new DocumentNumberService();
    private QuoteService service;

    @BeforeEach
    void setUp() {
        // Real calculation and numbering, fake database
        service = new QuoteService(quoteRepository, orderRepository, customerRepository,
                pricing, numbers, new LineMapper(productRepository));
    }

    /** A quote already saved, with 2 lines and its totals calculated */
    private Quote quote(QuoteStatus status) {
        Quote quote = new Quote();
        quote.setId(1L);
        quote.setNumber("DEV-2026-0069");
        quote.setCustomer(customer());
        quote.setStatus(status);
        quote.setValidUntil(LocalDate.now().plusDays(30));
        quote.addLine(quoteLine("Friteuse double à gaz", 1, "6000.00"));
        quote.addLine(quoteLine("Étagère Inox", 4, "800.00"));
        pricing.computeTotals(quote, quote.getLines());
        return quote;
    }

    @Test
    @DisplayName("Create: a line with only a productId takes the product's name and TTC price")
    void createUsesProductDefaults() {
        Product oven = new Product();
        oven.setId(5L);
        oven.setDesignation("Four mixte 6 niveaux");
        oven.setUnitPriceTTC(money("54000.00"));
        oven.setVatRate(money("20.00"));
        String prefix = numbers.yearPrefix("DEV");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findById(5L)).thenReturn(Optional.of(oven));
        when(quoteRepository.findTopByNumberStartingWithOrderByNumberDesc(prefix))
                .thenReturn(Optional.of(quoteWithNumber(prefix + "0041")));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(call -> call.getArgument(0));

        Quote created = service.create(1L, List.of(new LineInput(5L, null, 2, null, null)), null, null);

        assertThat(created.getLines()).hasSize(1);
        assertThat(created.getLines().get(0).getDesignation()).isEqualTo("Four mixte 6 niveaux");
        assertThat(created.getTotalTTC()).isEqualByComparingTo("108000.00");
        assertThat(created.getTotalHT()).isEqualByComparingTo("90000.00");
        assertThat(created.getNumber()).isEqualTo(prefix + "0042");
        assertThat(created.getStatus()).isEqualTo(QuoteStatus.DRAFT);
    }

    @Test
    @DisplayName("Convert: an accepted quote becomes an order with the same lines and totals")
    void convertAcceptedQuote() {
        Quote quote = quote(QuoteStatus.ACCEPTED);
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote));
        when(orderRepository.existsByQuoteId(1L)).thenReturn(false);
        when(orderRepository.findTopByNumberStartingWithOrderByNumberDesc(any())).thenReturn(Optional.empty());
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));

        CustomerOrder order = service.convertToOrder(1L);

        assertThat(order.getLines()).hasSize(2);
        assertThat(order.getTotalTTC()).isEqualByComparingTo("9200.00");
        assertThat(order.getTotalTTC()).isEqualByComparingTo(quote.getTotalTTC());
        assertThat(order.getQuote()).isSameAs(quote);
        assertThat(order.getNumber()).isEqualTo(numbers.yearPrefix("CMD") + "0001");
        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CONVERTED);
    }

    @Test
    @DisplayName("Convert: a quote that isn't accepted is refused")
    void convertDraftQuoteRefused() {
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote(QuoteStatus.DRAFT)));

        assertThatThrownBy(() -> service.convertToOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("DRAFT");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Convert: a quote can't be converted twice")
    void convertTwiceRefused() {
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote(QuoteStatus.ACCEPTED)));
        when(orderRepository.existsByQuoteId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.convertToOrder(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an order");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Accept: an expired quote is refused")
    void acceptExpiredQuoteRefused() {
        Quote expired = quote(QuoteStatus.SENT);
        expired.setValidUntil(LocalDate.now().minusDays(1));
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.accept(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
        assertThat(expired.getStatus()).isEqualTo(QuoteStatus.SENT);
    }

    @Test
    @DisplayName("Accept: a sent quote can be accepted")
    void acceptSentQuote() {
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote(QuoteStatus.SENT)));

        assertThat(service.accept(1L).getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Reject: a converted quote can't change anymore")
    void rejectConvertedQuoteRefused() {
        when(quoteRepository.findById(1L)).thenReturn(Optional.of(quote(QuoteStatus.CONVERTED)));

        assertThatThrownBy(() -> service.reject(1L)).isInstanceOf(BusinessException.class);
    }

    private static Quote quoteWithNumber(String number) {
        Quote q = new Quote();
        q.setNumber(number);
        return q;
    }
}
