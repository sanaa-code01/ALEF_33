package com.alef33.pdf;

/** The company printed at the top and bottom of every document (from application.properties) */
public record CompanyInfo(
        String name,
        String legalForm,      // SARL
        String activity,
        String headOffice,     // siège social
        String phone,
        String email,
        String rc,
        String patente,
        String identifiantFiscal,
        String ice,
        String bankAccount
) {}
