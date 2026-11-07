package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.model.Payment;
import com.alef33.model.enums.InvoiceStatus;
import com.alef33.model.enums.OrderStatus;
import com.alef33.model.enums.PaymentMethod;
import com.alef33.model.enums.PaymentType;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.PaymentRepository;
import com.alef33.service.dto.PaymentInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.alef33.service.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payments: deposit, installments, balance and invoice status")
class PaymentServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock OrderRepository orderRepository;
    @Mock InvoiceRepository invoiceRepository;
    @InjectMocks PaymentService service;

    private CustomerOrder order;

    @BeforeEach
    void setUp() {
        order = new CustomerOrder();
        order.setId(1L);
        order.setNumber("CMD-2026-0001");
        order.setStatus(OrderStatus.DELIVERED);
        order.setTotalTTC(money("68520.00"));
    }

    private static PaymentInput pay(String amount, PaymentMethod method) {
        return new PaymentInput(money(amount), null, method, null, null, null);
    }

    @Test
    @DisplayName("First partial payment is labelled DEPOSIT and dated today")
    void firstPaymentIsDeposit() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderId(1L)).thenReturn(null);       // nothing paid yet
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.empty()); // no invoice yet

        Payment payment = service.addPayment(1L, pay("20556.00", PaymentMethod.CHEQUE));

        assertThat(payment.getType()).isEqualTo(PaymentType.DEPOSIT);
        assertThat(payment.getPaymentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Paying everything at once is labelled FULL")
    void fullPayment() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderId(1L)).thenReturn(null);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.empty());

        assertThat(service.addPayment(1L, pay("68520.00", PaymentMethod.BANK_TRANSFER)).getType())
                .isEqualTo(PaymentType.FULL);
    }

    @Test
    @DisplayName("Last payment is labelled BALANCE and the invoice becomes PAID")
    void balancePaysTheInvoice() {
        Invoice invoice = new Invoice();
        invoice.setTotalTTC(money("68520.00"));
        invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderId(1L))
                .thenReturn(money("40556.00"))     // before this payment
                .thenReturn(money("68520.00"));    // after it
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
        when(invoiceRepository.findByOrderId(1L)).thenReturn(Optional.of(invoice));

        Payment payment = service.addPayment(1L, pay("27964.00", PaymentMethod.BANK_TRANSFER));

        assertThat(payment.getType()).isEqualTo(PaymentType.BALANCE);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Paying more than the remaining amount is refused")
    void overpaymentRefused() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.sumAmountByOrderId(1L)).thenReturn(money("40556.00"));

        assertThatThrownBy(() -> service.addPayment(1L, pay("99999.00", PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("27964.00");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A payment of 0 is refused")
    void zeroAmountRefused() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.addPayment(1L, pay("0", PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A cancelled order can't receive payments")
    void cancelledOrderRefused() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.addPayment(1L, pay("1000.00", PaymentMethod.CASH)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("Invoice status follows what has been paid")
    void invoiceStatusRule() {
        assertThat(service.statusFor(money("0"), money("68520.00"))).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(service.statusFor(money("20556.00"), money("68520.00"))).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(service.statusFor(money("68520.00"), money("68520.00"))).isEqualTo(InvoiceStatus.PAID);
    }
}
