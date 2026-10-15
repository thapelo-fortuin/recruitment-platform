package com.recruitplatform.soc_service.security;

import com.recruitplatform.soc_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminJwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public AdminJwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Internal services need to submit security events; all SOC reads and investigations are admin-only.
        return ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/events".equals(request.getRequestURI()))
                || ("GET".equalsIgnoreCase(request.getMethod()) && "/health".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Administrator sign-in required");
            return;
        }
        String token = header.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }
        if (!"ADMIN".equals(jwtUtil.extractRole(token))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator access required");
            return;
        }
        chain.doFilter(request, response);
    }
}
