package com.cap.stone.controller;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/opensearch")
public class OpenSearchController {

    @GetMapping("/**")
    public void redirectToOpenSearch(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        
        // Redirect to the actual OpenSearch dashboard (relative URL)
        response.sendRedirect("/opensearch-internal");
        //response.setHeader("X-Opensearch-Auth", "secretnolooking");
        //response.setHeader("X-Accel-Redirect", "/opensearch-internal");
    }
}