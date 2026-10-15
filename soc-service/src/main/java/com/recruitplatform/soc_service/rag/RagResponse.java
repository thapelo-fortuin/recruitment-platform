package com.recruitplatform.soc_service.rag;

import java.util.Collection;
import java.util.Map;

public record RagResponse(
        String question,
        String answer,
        String method,
        Collection<Map<String, Object>> evidence
) {}