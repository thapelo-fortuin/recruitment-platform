package com.recruitplatform.soc_service.controller;

import com.recruitplatform.soc_service.model.Alert;
import com.recruitplatform.soc_service.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public List<Alert> getAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }
}