package com.recruitplatform.job_service.soc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)   // before Spring Security, so its 401/403 responses are visible
public class ErrorAuditFilter extends OncePerRequestFilter {

    private static final String SERVICE_NAME = "job-service";

    private final SocEventClient socClient;

    public ErrorAuditFilter(SocEventClient socClient) {
        this.socClient = socClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            report(request, response.getStatus());
        }
    }

    private void report(HttpServletRequest request, int status) {
        if (status < 400) {
            return;   // the gateway already records normal traffic; here we only report problems
        }

        String eventType;
        String severity;
        if (status == 401 || status == 403) {
            eventType = "UNAUTHORISED_ACCESS";
            severity = "MEDIUM";
        } else if (status >= 500) {
            eventType = "SERVICE_ERROR";
            severity = "HIGH";
        } else if (status == 404) {
            eventType = "RESOURCE_NOT_FOUND";
            severity = "LOW";
        } else if (status == 400) {
            eventType = "VALIDATION_FAILURE";
            severity = "LOW";
        } else {
            return;   // other 4xx codes are ignored for now
        }

        String path = request.getRequestURI();
        socClient.send(new SocEvent(
                null,
                SERVICE_NAME,
                eventType,
                severity,
                null,                                   // this filter runs before authentication, so no user yet
                clientIp(request),
                path,
                request.getMethod(),
                status,
                request.getMethod() + " " + path + " -> " + status,
                request.getHeader("X-Correlation-Id"),
                resourceOf(path)));
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Client-Ip");
        return (ip != null && !ip.isBlank()) ? ip : request.getRemoteAddr();
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
}