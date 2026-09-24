package com.recruitplatform.api_gateway.soc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // run before Spring Security so we also see its 401/403 responses
public class CorrelationAuditFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final String SERVICE_NAME = "api-gateway";

    private final SocEventClient socClient;

    public CorrelationAuditFilter(SocEventClient socClient) {
        this.socClient = socClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Always generate our own ID; never trust one sent by the client.
        String correlationId = "corr-" + UUID.randomUUID();
        response.setHeader(HEADER, correlationId);

        try {
            chain.doFilter(new CorrelationRequestWrapper(request, correlationId), response);
        } finally {
            report(request, response.getStatus(), correlationId);
        }
    }

    private void report(HttpServletRequest request, int status, String correlationId) {
        String eventType;
        String severity;
        if (status == 401 || status == 403) {
            eventType = "UNAUTHORISED_ACCESS";
            severity = "MEDIUM";
        } else if (status >= 500) {
            eventType = "SERVICE_ERROR";
            severity = "HIGH";
        } else {
            eventType = "API_REQUEST";
            severity = "INFO";
        }

        String path = request.getRequestURI();
        String message = request.getMethod() + " " + path + " -> " + status;

        socClient.send(new SocEvent(
                Instant.now().toString(),
                SERVICE_NAME,
                eventType,
                severity,
                null,                       // gateway doesn't know the user; other services will
                clientIp(request),
                path,
                request.getMethod(),
                status,
                message,
                correlationId,
                resourceOf(path)));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resourceOf(String path) {
        String prefix = "/api/v1/";
        if (path.startsWith(prefix)) {
            String rest = path.substring(prefix.length());
            int slash = rest.indexOf('/');
            return slash > 0 ? rest.substring(0, slash) : rest;
        }
        return path;
    }

    /** Servlet request headers are read-only, so we wrap the request to add our header. */
    private static class CorrelationRequestWrapper extends HttpServletRequestWrapper {

        private final String correlationId;

        CorrelationRequestWrapper(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            if (HEADER.equalsIgnoreCase(name)) {
                return correlationId;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (HEADER.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(correlationId));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            for (String n : Collections.list(super.getHeaderNames())) {
                if (!HEADER.equalsIgnoreCase(n)) {
                    names.add(n);
                }
            }
            names.add(HEADER);
            return Collections.enumeration(names);
        }
    }
}