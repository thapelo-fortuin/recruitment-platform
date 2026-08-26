package com.recruitplatform.application_service.config;
import com.recruitplatform.application_service.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http .csrf(csrf -> csrf.disable()) .authorizeHttpRequests(auth -> auth .requestMatchers("/actuator/**").permitAll() .requestMatchers(HttpMethod.POST,
                "/api/applications").hasRole("CANDIDATE") .requestMatchers(HttpMethod.PUT,
                "/api/applications/**").hasRole("RECRUITER") .requestMatchers(HttpMethod.GET,
                "/api/applications/**").authenticated() .anyRequest().authenticated() ) .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
