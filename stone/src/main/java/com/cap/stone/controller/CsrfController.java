package com.cap.stone.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//Provides CSRF token endpoint for react to obtain CSRF protection tokens.
@RestController
public class CsrfController {
    @GetMapping("/api/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token; //spring automatically serializes to json
    }
}
