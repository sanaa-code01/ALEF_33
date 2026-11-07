package com.alef33.config;

import com.alef33.exception.BusinessException;
import com.alef33.model.Customer;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.model.Product;
import com.alef33.model.Quote;
import com.alef33.model.enums.OrderStatus;
import com.alef33.model.enums.PaymentMethod;
import com.alef33.repository.CustomerRepository;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.ProductRepository;
import com.alef33.service.OrderService;
import com.alef33.service.PaymentService;
import com.alef33.service.PricingService;
import com.alef33.service.QuoteService;
import com.alef33.service.dto.LineInput;
import com.alef33.service.dto.PaymentInput;
import com.alef33.service.dto.PaymentSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataRunner implements CommandLineRunner {

    private static final BigDecimal VAT_20 = new BigDecimal("20.00");

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InvoiceRepository invoiceRepository;
    private final QuoteService quoteService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Demo data already present, skipping");
            return;
        }

        // ── Customer and products (prices TTC, like ALEF 33) ──
        Customer customer = new Customer();
        customer.setName("Hôtel Atlas Agadir");
        customer.setCity("Agadir");
        customer.setEmail("achats@hotel-atlas.example");
        customer = customerRepository.save(customer);

        Product oven = product("FOUR-6N", "Four mixte 6 niveaux", "54000.00");
        Product table = product("TABLE-INOX-180", "Table inox 180 cm", "3840.00");

        // ── Quote → order ─────────────────────────────────────
        Quote quote = quoteService.create(customer.getId(), List.of(
                new LineInput(oven.getId(), null, 1, null, null),
                new LineInput(table.getId(), null, 3, null, null),
                new LineInput(null, "Installation et mise en service", 1, new BigDecimal("3000.00"), null)
        ), null, "Livraison sous 3 semaines");
        log.info("DEMO | Quote {} created: HT={} VAT={} TTC={}",
                quote.getNumber(), quote.getTotalHT(), quote.getTotalVat(), quote.getTotalTTC());

        quoteService.send(quote.getId());
        quoteService.accept(quote.getId());

        CustomerOrder order = quoteService.convertToOrder(quote.getId());
        Long orderId = order.getId();
        log.info("DEMO | Order {} created from quote, TTC={}", order.getNumber(), order.getTotalTTC());

        // ── 30 % deposit at order (before the invoice exists) ──
        paymentService.addPayment(orderId, new PaymentInput(
                new BigDecimal("20556.00"), null, PaymentMethod.CHEQUE, null, "CHQ-458921", "Acompte 30 %"));
        logSummary("after deposit", orderId);

        // ── Production, then invoice ──────────────────────────
        orderService.changeStatus(orderId, OrderStatus.IN_PRODUCTION);
        Invoice invoice = orderService.createInvoice(orderId);
        log.info("DEMO | Invoice {} created: TTC={} status={}",
                invoice.getNumber(), invoice.getTotalTTC(), statusOf(invoice.getId()));

        // ── Delivery, then one installment ────────────────────
        orderService.changeStatus(orderId, OrderStatus.DELIVERED);
        paymentService.addPayment(orderId, new PaymentInput(
                new BigDecimal("20000.00"), null, PaymentMethod.BANK_TRANSFER, null, null, "Mensualité 1"));
        logSummary("after installment", orderId);

        // ── Rule test: paying too much is refused ─────────────
        try {
            paymentService.addPayment(orderId, new PaymentInput(
                    new BigDecimal("99999.00"), null, PaymentMethod.CASH, null, null, null));
        } catch (BusinessException e) {
            log.info("DEMO | Overpayment refused as expected: {}", e.getMessage());
        }

        // ── Balance ───────────────────────────────────────────
        paymentService.addPayment(orderId, new PaymentInput(
                new BigDecimal("27964.00"), null, PaymentMethod.BANK_TRANSFER, null, null, "Solde"));
        logSummary("after balance", orderId);
        log.info("DEMO | Invoice {} final status = {}", invoice.getNumber(), statusOf(invoice.getId()));
    }

    private Product product(String reference, String designation, String priceTTC) {
        Product p = new Product();
        p.setReference(reference);
        p.setDesignation(designation);
        p.setUnitPriceTTC(new BigDecimal(priceTTC));
        p.setVatRate(VAT_20);
        p.setUnitPriceHT(PricingService.htFromTtc(p.getUnitPriceTTC(), VAT_20));
        return productRepository.save(p);
    }

    private void logSummary(String moment, Long orderId) {
        PaymentSummary s = paymentService.summary(orderId);
        log.info("DEMO | {} {}: total={} paid={} remaining={}",
                s.orderNumber(), moment, s.totalTTC(), s.paid(), s.remaining());
    }

    private Object statusOf(Long invoiceId) {
        return invoiceRepository.findById(invoiceId).map(Invoice::getStatus).orElse(null);
    }
}
