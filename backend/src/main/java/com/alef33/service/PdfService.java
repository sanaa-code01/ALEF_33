package com.alef33.service;

import com.alef33.exception.ResourceNotFoundException;
import com.alef33.model.Customer;
import com.alef33.model.DocumentLine;
import com.alef33.model.Invoice;
import com.alef33.model.Quote;
import com.alef33.model.enums.PaymentMethod;
import com.alef33.model.enums.PaymentType;
import com.alef33.pdf.CompanyInfo;
import com.alef33.pdf.PdfDocumentData;
import com.alef33.pdf.PdfRenderer;
import com.alef33.repository.InvoiceRepository;
import com.alef33.repository.PaymentRepository;
import com.alef33.repository.QuoteRepository;
import com.alef33.service.dto.PaymentSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Prepares the data of a quote or invoice, then asks the renderer to draw the PDF */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PdfService {

    private final QuoteRepository quoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final PdfRenderer renderer;

    @Value("${app.company.name}")
    private String companyName;
    @Value("${app.company.legal-form}")
    private String companyLegalForm;
    @Value("${app.company.activity}")
    private String companyActivity;
    @Value("${app.company.head-office}")
    private String companyHeadOffice;
    @Value("${app.company.phone}")
    private String companyPhone;
    @Value("${app.company.email}")
    private String companyEmail;
    @Value("${app.company.rc}")
    private String companyRc;
    @Value("${app.company.patente}")
    private String companyPatente;
    @Value("${app.company.if}")
    private String companyIf;
    @Value("${app.company.ice}")
    private String companyIce;
    @Value("${app.company.bank-account:}")
    private String companyBankAccount;

    public PdfFile quotePdf(Long id) {
        Quote q = quoteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quote", id));

        PdfDocumentData data = new PdfDocumentData(
                "Devis", q.getNumber(), q.getIssueDate(),
                "Valable jusqu'au", q.getValidUntil(), null,
                customer(q.getCustomer()), lines(q.getLines()),
                q.getTotalHT(), q.getTotalVat(), q.getTotalTTC(), false, q.getNotes(),
                List.of(), null, null,
                "Arrêté le présent devis à la somme de :");

        return new PdfFile(q.getNumber() + ".pdf", renderer.render(data, company()));
    }

    public PdfFile invoicePdf(Long id) {
        Invoice i = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        Long orderId = i.getOrder().getId();
        PaymentSummary summary = paymentService.summary(orderId);

        List<PdfDocumentData.PaymentRow> payments = paymentRepository.findByOrderIdOrderByPaymentDateAsc(orderId)
                .stream()
                .map(p -> new PdfDocumentData.PaymentRow(p.getPaymentDate(), label(p.getType()),
                        label(p.getMethod()), p.getReference(), p.getAmount()))
                .toList();

        PdfDocumentData data = new PdfDocumentData(
                "Facture", i.getNumber(), i.getIssueDate(),
                "Échéance", i.getDueDate(), "Commande : " + i.getOrder().getNumber(),
                customer(i.getCustomer()), lines(i.getLines()),
                i.getTotalHT(), i.getTotalVat(), i.getTotalTTC(), true, i.getNotes(),
                payments, summary.paid(), summary.remaining(),
                "Arrêtée la présente facture à la somme de :");

        return new PdfFile(i.getNumber() + ".pdf", renderer.render(data, company()));
    }

    public record PdfFile(String filename, byte[] content) {}

    // ── Mapping helpers ───────────────────────────────────
    private CompanyInfo company() {
        return new CompanyInfo(companyName, companyLegalForm, companyActivity, companyHeadOffice,
                companyPhone, companyEmail, companyRc, companyPatente, companyIf, companyIce,
                companyBankAccount);
    }

    private PdfDocumentData.Customer customer(Customer c) {
        return new PdfDocumentData.Customer(c.getName(), c.getIce(), c.getAddress(),
                c.getCity(), c.getPhone(), c.getEmail());
    }

    private List<PdfDocumentData.Line> lines(List<? extends DocumentLine> lines) {
        return lines.stream()
                .map(l -> new PdfDocumentData.Line(l.getDesignation(), l.getQuantity(),
                        l.getUnitPriceTTC(), l.getVatRate(), l.getLineTotalTTC()))
                .toList();
    }

    // French labels printed on the documents
    private static String label(PaymentType t) {
        return switch (t) {
            case DEPOSIT -> "Acompte";
            case INSTALLMENT -> "Mensualité";
            case BALANCE -> "Solde";
            case FULL -> "Paiement total";
        };
    }

    private static String label(PaymentMethod m) {
        return switch (m) {
            case CASH -> "Espèces";
            case CHEQUE -> "Chèque";
            case BANK_TRANSFER -> "Virement";
            case CARD -> "Carte bancaire";
            case BILL_OF_EXCHANGE -> "Effet de commerce";
        };
    }
}
