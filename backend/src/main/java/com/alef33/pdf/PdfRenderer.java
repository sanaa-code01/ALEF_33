package com.alef33.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Draws a quote or an invoice as an A4 PDF, in the layout of ALEF 33's own documents */
@Component
public class PdfRenderer {

    // Colors taken from ALEF 33's Word template
    private static final Color ORANGE = new Color(0xDD, 0x80, 0x46);
    private static final Color TEXT = new Color(0x52, 0x5A, 0x39);
    private static final Color DARK = new Color(33, 33, 33);
    private static final Color BORDER = new Color(90, 90, 90);
    private static final Color LIGHT = new Color(253, 242, 235);

    private static final Font DOC_TITLE = font(FontFactory.HELVETICA_BOLD, 15, TEXT);
    private static final Font DOC_INFO = font(FontFactory.HELVETICA_BOLD, 12, TEXT);
    private static final Font CLIENT = font(FontFactory.HELVETICA, 12, TEXT);
    private static final Font ACTIVITY = font(FontFactory.HELVETICA, 10, ORANGE);
    private static final Font SMALL = font(FontFactory.HELVETICA, 8, DARK);
    private static final Font TH = font(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private static final Font N = font(FontFactory.HELVETICA, 9.5f, DARK);
    private static final Font B = font(FontFactory.HELVETICA_BOLD, 9.5f, DARK);
    private static final Font TOTAL_LABEL = font(FontFactory.HELVETICA_BOLD, 9, TEXT);
    private static final Font TOTAL_VALUE = font(FontFactory.HELVETICA_BOLD, 10.5f, DARK);
    private static final Font FOOTER = font(FontFactory.HELVETICA, 6.5f, DARK);

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern NUMBER = Pattern.compile("^[A-Z]+-(\\d{4})-(\\d+)$");

    private final byte[] logo = resource("/pdf/logo-alef33.jpg");
    private final byte[] footerBar = resource("/pdf/footer-bar.png");

    public byte[] render(PdfDocumentData data, CompanyInfo company) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 36, 36, 30, 80);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setPageEvent(new Footer(company, footerBar));
        doc.addTitle(data.title() + " " + data.number());
        doc.addCreator(company.name());
        doc.open();

        doc.add(header(company));
        doc.add(spacer(8));
        doc.add(infoBlock(data));
        doc.add(spacer(10));
        doc.add(linesTable(data));
        doc.add(totalsTable(data));

        doc.add(spacer(12));
        Paragraph words = new Paragraph();
        words.add(new Chunk(data.amountInWordsLabel() + "   ", N));
        words.add(new Chunk(AmountInWords.dirhams(data.totalTTC()), N));
        words.setIndentationLeft(18);
        doc.add(words);

        if (data.paid() != null) {
            doc.add(spacer(10));
            Paragraph title = new Paragraph("Règlements", B);
            title.setIndentationLeft(18);
            doc.add(title);
            doc.add(spacer(4));
            doc.add(paymentsTable(data));
        }

        if (data.notes() != null && !data.notes().isBlank()) {
            doc.add(spacer(12));
            Paragraph notes = new Paragraph();
            notes.add(new Chunk("Remarques : ", B));
            notes.add(new Chunk(data.notes(), N));
            notes.setIndentationLeft(18);
            doc.add(notes);
        }

        doc.add(spacer(data.paid() != null ? 14 : 30));
        Paragraph signature = new Paragraph("SIGNATURE :", B);
        signature.setIndentationLeft(300);
        doc.add(signature);

        doc.close();
        return out.toByteArray();
    }

    // ── Logo, legal form and activity ─────────────────────
    private PdfPTable header(CompanyInfo company) {
        PdfPTable t = table(new float[]{100});
        PdfPCell cell = noBorder();
        if (logo != null) {
            Image img = image(logo);
            img.scaleToFit(150, 60);
            cell.addElement(img);
        } else {
            cell.addElement(new Paragraph(company.name(), DOC_TITLE));
        }
        Paragraph legal = new Paragraph(company.legalForm(), SMALL);
        legal.setIndentationLeft(110);
        cell.addElement(legal);
        Paragraph activity = new Paragraph(company.activity(), ACTIVITY);
        activity.setIndentationLeft(110);
        cell.addElement(activity);
        t.addCell(cell);
        return t;
    }

    // ── "Devis N° ... / DATE" on the left, "Client : ..." on the right ──
    private PdfPTable infoBlock(PdfDocumentData data) {
        PdfPTable t = table(new float[]{55, 45});

        PdfPCell left = noBorder();
        left.setPaddingLeft(18);
        left.addElement(new Paragraph(data.title() + " N° " + displayNumber(data.number()), DOC_TITLE));
        left.addElement(spacer(6));
        left.addElement(new Paragraph("DATE : " + date(data.issueDate()), DOC_INFO));
        if (data.secondDate() != null) {
            left.addElement(new Paragraph(data.secondDateLabel() + " : " + date(data.secondDate()), N));
        }
        if (data.reference() != null) {
            left.addElement(new Paragraph(data.reference(), N));
        }
        t.addCell(left);

        PdfDocumentData.Customer c = data.customer();
        PdfPCell right = noBorder();
        Paragraph client = new Paragraph();
        client.add(new Chunk("Client :   ", DOC_TITLE));
        client.add(new Chunk(c.name(), DOC_INFO));
        right.addElement(client);
        right.addElement(spacer(6));
        String place = join(c.address(), c.city());
        if (!place.isBlank()) right.addElement(new Paragraph(place, CLIENT));
        if (notBlank(c.phone())) right.addElement(new Paragraph("Tél : " + c.phone(), N));
        if (notBlank(c.ice())) right.addElement(new Paragraph("ICE : " + c.ice(), N));
        t.addCell(right);
        return t;
    }

    // ── Lines: Désignation | Qté | P. U TTC | Total TTC ──
    private PdfPTable linesTable(PdfDocumentData data) {
        PdfPTable t = table(new float[]{52, 10, 19, 19});
        t.setHeaderRows(1);
        for (String h : new String[]{"Désignation", "Qté", "P. U TTC", "Total TTC"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TH));
            cell.setBackgroundColor(ORANGE);
            cell.setBorderColor(BORDER);
            cell.setPadding(7);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(cell);
        }
        for (PdfDocumentData.Line l : data.lines()) {
            t.addCell(bodyCell(l.designation()));
            t.addCell(bodyCell(String.valueOf(l.quantity())));
            t.addCell(bodyCell(money(l.unitPriceTTC())));
            t.addCell(bodyCell(money(l.lineTotalTTC())));
        }
        return t;
    }

    // ── Totals under the last two columns ─────────────────
    private PdfPTable totalsTable(PdfDocumentData data) {
        PdfPTable t = table(new float[]{52, 29, 19});
        if (data.showVatBreakdown()) {
            totalRow(t, "TOTAL HT", money(data.totalHT()) + " DH", false);
            totalRow(t, vatLabel(data), money(data.totalVat()) + " DH", false);
        }
        totalRow(t, "TOTAL TTC", money(data.totalTTC()) + " DH", true);
        if (data.paid() != null) {
            totalRow(t, "DÉJÀ RÉGLÉ", money(data.paid()) + " DH", false);
            totalRow(t, "RESTE À PAYER", money(data.remaining()) + " DH", true);
        }
        return t;
    }

    private void totalRow(PdfPTable t, String label, String value, boolean highlight) {
        t.addCell(noBorder());
        PdfPCell l = new PdfPCell(new Phrase(label, TOTAL_LABEL));
        PdfPCell v = new PdfPCell(new Phrase(value, TOTAL_VALUE));
        for (PdfPCell c : new PdfPCell[]{l, v}) {
            c.setBorderColor(BORDER);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            if (highlight) c.setBackgroundColor(LIGHT);
        }
        t.addCell(l);
        t.addCell(v);
    }

    // ── Payments (invoices only) ──────────────────────────
    private PdfPTable paymentsTable(PdfDocumentData data) {
        PdfPTable t = table(new float[]{16, 18, 22, 24, 20});
        if (data.payments().isEmpty()) {
            PdfPCell none = new PdfPCell(new Phrase("Aucun règlement enregistré.", N));
            none.setColspan(5);
            none.setBorderColor(BORDER);
            none.setPadding(6);
            t.addCell(none);
            return t;
        }
        t.setHeaderRows(1);
        for (String h : new String[]{"Date", "Type", "Mode", "Référence", "Montant"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TH));
            cell.setBackgroundColor(ORANGE);
            cell.setBorderColor(BORDER);
            cell.setPadding(5);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            t.addCell(cell);
        }
        for (PdfDocumentData.PaymentRow p : data.payments()) {
            t.addCell(bodyCell(date(p.date())));
            t.addCell(bodyCell(p.type()));
            t.addCell(bodyCell(p.method()));
            t.addCell(bodyCell(p.reference() != null ? p.reference() : "-"));
            t.addCell(bodyCell(money(p.amount())));
        }
        return t;
    }

    // ── Footer on every page: colored bar + legal lines ───
    private static class Footer extends PdfPageEventHelper {
        private final CompanyInfo company;
        private final byte[] bar;

        Footer(CompanyInfo company, byte[] bar) {
            this.company = company;
            this.bar = bar;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float width = document.right() - document.left();
            float barY = 52;

            if (bar != null) {
                Image img = image(bar);
                img.scaleAbsolute(width, 5);
                img.setAbsolutePosition(document.left(), barY);
                cb.addImage(img);
            } else {
                cb.setColorFill(ORANGE);
                cb.rectangle(document.left(), barY, width, 4);
                cb.fill();
            }

            float center = (document.left() + document.right()) / 2;
            String line1 = "Siège social : " + company.headOffice()
                    + " – GSM : " + company.phone() + " – Email : " + company.email();
            String line2 = "RC : " + company.rc() + " / PATENTE : " + company.patente()
                    + " / IF : " + company.identifiantFiscal() + " – ICE : " + company.ice()
                    + (notBlank(company.bankAccount()) ? " – CB : " + company.bankAccount() : "");
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(line1, FOOTER), center, barY - 12, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(line2, FOOTER), center, barY - 22, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), FOOTER), document.right(), barY - 34, 0);
        }
    }

    // ── Helpers ───────────────────────────────────────────
    /** "DEV-2026-0069" → "0069/26", like on ALEF 33's documents */
    static String displayNumber(String number) {
        Matcher m = NUMBER.matcher(number);
        return m.matches() ? m.group(2) + "/" + m.group(1).substring(2) : number;
    }

    private static String vatLabel(PdfDocumentData data) {
        long rates = data.lines().stream().map(l -> l.vatRate().stripTrailingZeros()).distinct().count();
        if (rates == 1) {
            String rate = data.lines().get(0).vatRate().stripTrailingZeros().toPlainString().replace('.', ',');
            return "TVA " + rate + " %";
        }
        return "TVA";
    }

    private static Font font(String name, float size, Color color) {
        Font f = FontFactory.getFont(name, size);
        f.setColor(color);
        return f;
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        return t;
    }

    private static PdfPCell noBorder() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(0);
        return c;
    }

    private static PdfPCell bodyCell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, N));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorderColor(BORDER);
        c.setPadding(5);
        c.setPaddingBottom(6);
        return c;
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ", N);
        p.setLeading(height);
        return p;
    }

    /** 95800 → "95 800,00" (regular space: the PDF base fonts have no narrow space) */
    static String money(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(value.setScale(2, RoundingMode.HALF_UP));
    }

    private static String date(LocalDate d) {
        return d != null ? d.format(DATE) : "-";
    }

    private static String join(String a, String b) {
        if (notBlank(a) && notBlank(b)) return a + ", " + b;
        return notBlank(a) ? a : (notBlank(b) ? b : "");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static Image image(byte[] bytes) {
        try {
            return Image.getInstance(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read image", e);
        }
    }

    private static byte[] resource(String path) {
        try (InputStream in = PdfRenderer.class.getResourceAsStream(path)) {
            return in != null ? in.readAllBytes() : null;
        } catch (IOException e) {
            return null;
        }
    }
}
