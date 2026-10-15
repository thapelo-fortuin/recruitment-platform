package com.recruitplatform.soc_service.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    FilterRegistrationBean<AdminJwtFilter> disableContainerFilterRegistration(AdminJwtFilter adminJwtFilter) {
        FilterRegistrationBean<AdminJwtFilter> registration = new FilterRegistrationBean<>(adminJwtFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AdminJwtFilter adminJwtFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(adminJwtFilter, AnonymousAuthenticationFilter.class)
                .build();
    }
}
