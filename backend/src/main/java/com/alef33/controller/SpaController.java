package com.alef33.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the Angular app is served by Spring Boot (online demo), a refresh on /devis/2
 * must return index.html: Angular then shows the right page.
 * Paths with a dot (main.js, logo.jpg...) and the /api routes are not affected.
 */
@Controller
public class SpaController {

    @GetMapping({"/", "/{page:[^.]*}", "/{page:[^.]*}/{id:[^.]*}"})
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
