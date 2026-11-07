package com.alef33.controller;

import com.alef33.service.PdfService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Builds the HTTP response for a PDF: opens in the browser, with a clean file name */
final class PdfResponses {

    private PdfResponses() {}

    static ResponseEntity<byte[]> of(PdfService.PdfFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }
}
