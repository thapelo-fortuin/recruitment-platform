package com.recruitplatform.job_service.config;
import com.recruitplatform.job_service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http .csrf(csrf -> csrf.disable()) .authorizeHttpRequests(auth -> auth .requestMatchers("/actuator/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll() .requestMatchers(org.springframework.http.HttpMethod.GET,
                "/api/v1/jobs/**").authenticated() .requestMatchers(org.springframework.http.HttpMethod.POST,
                "/api/v1/jobs").hasRole("RECRUITER") .requestMatchers(org.springframework.http.HttpMethod.PUT,
                "/api/v1/jobs/**").hasRole("RECRUITER") .anyRequest().authenticated() ) .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}