package com.recruitplatform.api_gateway.soc;

public record SocEvent(
        String timestamp,
        String serviceName,
        String eventType,
        String severity,
        String userId,
        String sourceIp,
        String endpoint,
        String httpMethod,
        Integer statusCode,
        String message,
        String correlationId,
        String affectedEntity
) {}