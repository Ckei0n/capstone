package com.cap.stone.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


// Configures Spring Security with OAuth2/OIDC authentication 
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // Configure URL-based authorization rules
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/assets/**", "/static/**", "/favicon.ico", "/api/csrf").permitAll()
                .anyRequest().authenticated()
            )
            // default settings for OAuth2 login
            .oauth2Login(oauth2 -> {

            })
            // Configure logout behavior
            .logout(logout -> logout.logoutSuccessUrl("/"));
            
        return http.build();
    }
}