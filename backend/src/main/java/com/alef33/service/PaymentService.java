package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.model.Payment;
import com.alef33.model.enums.InvoiceStatus;
import com.alef33.model.enums.OrderStatus;
import com.alef33.model.enums.PaymentType;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.OrderRepository;
import com.alef33.repository.PaymentRepository;
import com.alef33.service.dto.PaymentInput;
import com.alef33.service.dto.PaymentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional
    public Payment addPayment(Long orderId, PaymentInput input) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("La commande " + order.getNumber() + " est annulée");
        }
        if (input.amount() == null || input.amount().signum() <= 0) {
            throw new BusinessException("Le montant du règlement doit être supérieur à 0");
        }
        if (input.method() == null) {
            throw new BusinessException("Le mode de règlement est obligatoire");
        }

        BigDecimal paid = paidFor(orderId);
        BigDecimal remaining = order.getTotalTTC().subtract(paid);

        if (input.amount().compareTo(remaining) > 0) {
            throw new BusinessException("Le règlement de " + input.amount()
                    + " DH dépasse le reste à payer (" + remaining + " DH)");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(input.amount());
        payment.setPaymentDate(input.paymentDate() != null ? input.paymentDate() : LocalDate.now());
        payment.setMethod(input.method());
        payment.setType(input.type() != null ? input.type() : guessType(input.amount(), paid, remaining));
        payment.setReference(input.reference());
        payment.setNotes(input.notes());
        paymentRepository.save(payment);

        refreshInvoiceStatus(orderId);
        return payment;
    }

    @Transactional
    public void deletePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
        Long orderId = payment.getOrder().getId();
        paymentRepository.delete(payment);
        paymentRepository.flush();
        refreshInvoiceStatus(orderId);
    }

    @Transactional(readOnly = true)
    public List<Payment> listForOrder(Long orderId) {
        return paymentRepository.findByOrderIdOrderByPaymentDateAsc(orderId);
    }

    @Transactional(readOnly = true)
    public PaymentSummary summary(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        BigDecimal paid = paidFor(orderId);
        return new PaymentSummary(order.getNumber(), order.getTotalTTC(), paid,
                order.getTotalTTC().subtract(paid));
    }

    @Transactional
    public void refreshInvoiceStatus(Long orderId) {
        invoiceRepository.findByOrderId(orderId).ifPresent(invoice -> {
            if (invoice.getStatus() == InvoiceStatus.CANCELLED) return;
            invoice.setStatus(statusFor(paidFor(orderId), invoice.getTotalTTC()));
        });
    }

    public InvoiceStatus statusFor(BigDecimal paid, BigDecimal total) {
        if (paid.signum() <= 0) return InvoiceStatus.UNPAID;
        if (paid.compareTo(total) >= 0) return InvoiceStatus.PAID;
        return InvoiceStatus.PARTIALLY_PAID;
    }

    private BigDecimal paidFor(Long orderId) {
        BigDecimal sum = paymentRepository.sumAmountByOrderId(orderId);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    private PaymentType guessType(BigDecimal amount, BigDecimal alreadyPaid, BigDecimal remaining) {
        boolean paysEverythingLeft = amount.compareTo(remaining) == 0;
        boolean firstPayment = alreadyPaid.signum() == 0;
        if (paysEverythingLeft && firstPayment) return PaymentType.FULL;
        if (paysEverythingLeft) return PaymentType.BALANCE;
        if (firstPayment) return PaymentType.DEPOSIT;
        return PaymentType.INSTALLMENT;
    }
}
