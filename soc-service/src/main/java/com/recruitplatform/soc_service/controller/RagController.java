package com.recruitplatform.soc_service.controller;

import com.recruitplatform.soc_service.rag.RagResponse;
import com.recruitplatform.soc_service.rag.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    public record AskRequest(String question) {}

    @PostMapping("/ask")
    public RagResponse ask(@RequestBody AskRequest request) {
        return ragService.ask(request.question());
    }
}