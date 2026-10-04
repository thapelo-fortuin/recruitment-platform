package com.recruitplatform.soc_service.rag;

import java.util.Optional;

public record RagIntent(
        QuestionIntent type,
        Optional<String> service,
        Optional<String> alertId,
        Optional<String> userId
) {}