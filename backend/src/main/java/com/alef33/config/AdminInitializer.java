package com.alef33.config;

import com.alef33.model.enums.Role;
import com.alef33.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Creates the first admin account at startup, so someone can log in on a new database */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.demo-data:false}")
    private boolean demoData;

    @Override
    public void run(String... args) {
        if (userService.createIfMissing(adminEmail, adminPassword, "Administrateur", Role.ADMIN)) {
            log.info("SECURITY | Admin account created: {}", adminEmail);
        }
        if (demoData && userService.createIfMissing("employe@alef33.ma", "Employe@2026",
                "Employé Démo", Role.EMPLOYEE)) {
            log.info("SECURITY | Demo employee account created: employe@alef33.ma");
        }
    }
}
