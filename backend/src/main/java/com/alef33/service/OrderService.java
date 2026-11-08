package com.alef33.service;

import com.alef33.exception.BusinessException;
import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.*;
import com.alef33.model.enums.OrderStatus;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.IN_PRODUCTION, OrderStatus.CANCELLED),
            OrderStatus.IN_PRODUCTION, Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED),
            OrderStatus.DELIVERED, Set.of(),
            OrderStatus.CANCELLED, Set.of()
    );

    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PricingService pricingService;
    private final DocumentNumberService numberService;
    private final LineMapper lineMapper;
    private final PaymentService paymentService;

    @Transactional(readOnly = true)
    public CustomerOrder get(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional
    public CustomerOrder changeStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order = get(id);
        if (!TRANSITIONS.get(order.getStatus()).contains(newStatus)) {
            throw new BusinessException("Commande " + order.getNumber() + " : impossible de passer de « "
                    + label(order.getStatus()) + " » à « " + label(newStatus) + " »");
        }
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveryDate(LocalDate.now());
        }
        return order;
    }

    @Transactional
    public Invoice createInvoice(Long orderId) {
        CustomerOrder order = get(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("La commande " + order.getNumber() + " est annulée");
        }
        if (invoiceRepository.existsByOrderId(orderId)) {
            throw new BusinessException("La commande " + order.getNumber() + " est déjà facturée");
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setCustomer(order.getCustomer());
        invoice.setNotes(order.getNotes());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        for (OrderLine line : order.getLines()) {
            invoice.addLine(lineMapper.copy(line, new InvoiceLine()));
        }
        pricingService.computeTotals(invoice, invoice.getLines());

        String prefix = numberService.yearPrefix("FAC");
        invoice.setNumber(numberService.next(prefix,
                invoiceRepository.findTopByNumberStartingWithOrderByNumberDesc(prefix).map(Invoice::getNumber)));

        invoiceRepository.save(invoice);

        paymentService.refreshInvoiceStatus(orderId);
        return invoice;
    }

    private static String label(OrderStatus status) {
        return switch (status) {
            case PENDING -> "En attente";
            case IN_PRODUCTION -> "En fabrication";
            case DELIVERED -> "Livrée";
            case CANCELLED -> "Annulée";
        };
    }
}
