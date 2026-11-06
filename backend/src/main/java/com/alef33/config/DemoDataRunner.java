package com.alef33.config;

import com.alef33.model.Customer;
import com.alef33.model.CustomerOrder;
import com.alef33.model.Invoice;
import com.alef33.model.Product;
import com.alef33.model.Quote;
import com.alef33.model.enums.OrderStatus;
import com.alef33.repository.CustomerRepository;
import com.alef33.repository.ProductRepository;
import com.alef33.service.OrderService;
import com.alef33.service.QuoteService;
import com.alef33.service.dto.LineInput;
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

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final QuoteService quoteService;
    private final OrderService orderService;

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Demo data already present, skipping");
            return;
        }

        Customer customer = new Customer();
        customer.setName("Hôtel Atlas Agadir");
        customer.setCity("Agadir");
        customer.setEmail("achats@hotel-atlas.example");
        customer = customerRepository.save(customer);

        Product oven = new Product();
        oven.setReference("FOUR-6N");
        oven.setDesignation("Four mixte 6 niveaux");
        oven.setUnitPriceHT(new BigDecimal("45000.00"));
        oven = productRepository.save(oven);

        Product table = new Product();
        table.setReference("TABLE-INOX-180");
        table.setDesignation("Table inox 180 cm");
        table.setUnitPriceHT(new BigDecimal("3200.00"));
        table = productRepository.save(table);

        // 1 oven + 3 tables + 1 custom line (installation) without product
        Quote quote = quoteService.create(customer.getId(), List.of(
                new LineInput(oven.getId(), null, 1, null, null),
                new LineInput(table.getId(), null, 3, null, null),
                new LineInput(null, "Installation et mise en service", 1, new BigDecimal("2500.00"), null)
        ), null, "Livraison sous 3 semaines");
        log.info("Quote {} created: HT={} VAT={} TTC={}",
                quote.getNumber(), quote.getTotalHT(), quote.getTotalVat(), quote.getTotalTTC());

        quoteService.send(quote.getId());
        quoteService.accept(quote.getId());

        CustomerOrder order = quoteService.convertToOrder(quote.getId());
        log.info("Order {} created from quote, TTC={}", order.getNumber(), order.getTotalTTC());

        orderService.changeStatus(order.getId(), OrderStatus.IN_PRODUCTION);

        Invoice invoice = orderService.createInvoice(order.getId());
        log.info("Invoice {} created: TTC={} status={}",
                invoice.getNumber(), invoice.getTotalTTC(), invoice.getStatus());
    }
}