package com.cap.stone.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/assets/**", "/static/**", "/favicon.ico", "/api/csrf", "api/auth/verify").permitAll()
                .requestMatchers("/opensearch/**").hasAuthority("admin")
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
            )
            .logout(logout -> logout.logoutSuccessUrl("/"));
            
        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            Set<SimpleGrantedAuthority> authorities = new HashSet<>();
            
            // Extract realm roles
            extractRealmRoles(oidcUser.getClaims(), authorities);
            
            // Extract client/resource roles
            extractResourceRoles(oidcUser.getClaims(), authorities);

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }
    
    private void extractRealmRoles(java.util.Map<String, Object> claims, Set<SimpleGrantedAuthority> authorities) {
        Object realmAccessObj = claims.get("realm_access");
        if (realmAccessObj instanceof java.util.Map<?, ?> realmAccess) {
            Object rolesObj = realmAccess.get("roles");
            if (rolesObj instanceof List<?> rolesList) {
                rolesList.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
            }
        }
    }
    
    private void extractResourceRoles(java.util.Map<String, Object> claims, Set<SimpleGrantedAuthority> authorities) {
        Object resourceAccessObj = claims.get("resource_access");
        if (resourceAccessObj instanceof java.util.Map<?, ?> resourceAccess) {
            resourceAccess.values().forEach(clientRoles -> {
                if (clientRoles instanceof java.util.Map<?, ?> clientRoleMap) {
                    Object rolesObj = clientRoleMap.get("roles");
                    if (rolesObj instanceof List<?> rolesList) {
                        rolesList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(SimpleGrantedAuthority::new)
                            .forEach(authorities::add);
                    }
                }
            });
        }
    }
}