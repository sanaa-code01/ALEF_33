package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.model.OrderLine;
import com.alef33.model.enums.InvoiceStatus;
import com.alef33.model.enums.OrderStatus;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.alef33.service.TestData.customer;
import static com.alef33.service.TestData.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Orders: status changes and invoice creation")
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock ProductRepository productRepository;
    @Mock PaymentService paymentService;

    private final PricingService pricing = new PricingService();
    private final DocumentNumberService numbers = new DocumentNumberService();
    private OrderService service;
    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, invoiceRepository, pricing, numbers,
                new LineMapper(productRepository), paymentService);

        order = new CustomerOrder();
        order.setId(1L);
        order.setNumber("CMD-2026-0001");
        order.setCustomer(customer());
        order.addLine(line(new OrderLine(), "Four mixte 6 niveaux", 1, "54000.00"));
        order.addLine(line(new OrderLine(), "Table inox 180 cm", 3, "3840.00"));
        pricing.computeTotals(order, order.getLines());
    }

    @Test
    @DisplayName("PENDING → IN_PRODUCTION → DELIVERED, with the delivery date set")
    void normalFlow() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.changeStatus(1L, OrderStatus.IN_PRODUCTION);
        service.changeStatus(1L, OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveryDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("A delivered order can't go back to PENDING or be cancelled")
    void deliveredIsFinal() {
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.changeStatus(1L, OrderStatus.PENDING))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.changeStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Invoice copies the order's lines and totals, then counts payments already made")
    void createInvoice() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(false);
        when(invoiceRepository.findTopByNumberStartingWithOrderByNumberDesc(any())).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));

        Invoice invoice = service.createInvoice(1L);

        assertThat(invoice.getLines()).hasSize(2);
        assertThat(invoice.getTotalTTC()).isEqualByComparingTo("65520.00");   // 54 000 + 3 × 3 840
        assertThat(invoice.getTotalHT()).isEqualByComparingTo("54600.00");
        assertThat(invoice.getNumber()).isEqualTo(numbers.yearPrefix("FAC") + "0001");
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(paymentService).refreshInvoiceStatus(1L);   // a deposit would be counted here
    }

    @Test
    @DisplayName("An order can only be invoiced once")
    void invoiceOnlyOnce() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createInvoice(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà facturée");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("A cancelled order can't be invoiced")
    void cancelledOrderNotInvoiced() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.createInvoice(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("annulée");
    }
}
