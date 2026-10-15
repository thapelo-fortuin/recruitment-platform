package com.recruitplatform.api_gateway.gateway.filter;

import com.recruitplatform.api_gateway.util.JwtUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthFilter implements Filter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // Authentication endpoints and the public job catalogue are available before sign-in.
        boolean publicJobRead = "GET".equalsIgnoreCase(request.getMethod())
                && path.matches("^/api/v1/jobs(?:/\\d+)?$");
        if ("/health".equals(path) || path.startsWith("/api/v1/auth/") || publicJobRead) {
            chain.doFilter(req, res);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing or malformed Authorization header");
            return;
        }

        String token = authHeader.substring(7); // strip "Bearer "

        if (!jwtUtil.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }

        // Security operations data is restricted to administrator accounts.
        if (path.startsWith("/api/v1/soc/") && !"ADMIN".equals(jwtUtil.extractRole(token))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Administrator access required");
            return;
        }

        // Token is valid — request continues to its destination
        chain.doFilter(req, res);
    }
}
