package com.recruitplatform.application_service.controller;

import com.recruitplatform.application_service.client.JobServiceClient;
import com.recruitplatform.application_service.dto.ApplicationRequest;
import com.recruitplatform.application_service.model.Application;
import com.recruitplatform.application_service.repository.ApplicationRepository;
import com.recruitplatform.application_service.soc.SocEvent;
import com.recruitplatform.application_service.soc.SocEventClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final JobServiceClient jobServiceClient;
    private final SocEventClient socClient;

    public ApplicationController(ApplicationRepository applicationRepository,
                                 JobServiceClient jobServiceClient,
                                 SocEventClient socClient) {
        this.applicationRepository = applicationRepository;
        this.jobServiceClient = jobServiceClient;
        this.socClient = socClient;
    }

    @PostMapping
    public ResponseEntity<?> apply(@Valid @RequestBody ApplicationRequest request,
                                   Authentication authentication,
                                   @RequestHeader("Authorization") String authHeader,
                                   HttpServletRequest httpRequest) {
        String candidateEmail = authentication.getName();

        if (applicationRepository.existsByCandidateEmailAndJobId(candidateEmail, request.getJobId())) {
            return ResponseEntity.badRequest().body("You have already applied to this job");
        }

        Map<String, Object> job = jobServiceClient.getJob(request.getJobId(), authHeader);
        if (job == null) {
            return ResponseEntity.badRequest().body("Job not found");
        }
        if (!"OPEN".equals(job.get("status"))) {
            return ResponseEntity.badRequest().body("This job is no longer accepting applications");
        }

        Application application = new Application();
        application.setCandidateEmail(candidateEmail);
        application.setJobId(request.getJobId());
        application.setStatus("APPLIED");
        application.setAppliedAt(LocalDateTime.now());
        applicationRepository.save(application);

        reportAction(httpRequest, candidateEmail, "Application submitted", application);
        return ResponseEntity.ok(application);
    }

    @GetMapping
    public List<Application> getMyApplications(Authentication authentication) {
        return applicationRepository.findByCandidateEmail(authentication.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getApplication(@PathVariable Long id) {
        Optional<Application> app = applicationRepository.findById(id);
        return app.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication authentication,
                                          HttpServletRequest httpRequest) {
        Optional<Application> appOpt = applicationRepository.findById(id);
        if (appOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Application application = appOpt.get();
        application.setStatus(body.get("status"));
        applicationRepository.save(application);

        reportAction(httpRequest, authentication.getName(),
                "Application status changed to " + application.getStatus(), application);
        return ResponseEntity.ok(application);
    }

    private void reportAction(HttpServletRequest http, String userEmail, String message, Application app) {
        String ip = http.getHeader("X-Client-Ip");
        if (ip == null || ip.isBlank()) {
            ip = http.getRemoteAddr();
        }
        socClient.send(new SocEvent(
                null, "application-service", "BUSINESS_ACTION", "INFO",
                userEmail, ip, http.getRequestURI(), http.getMethod(), 200,
                message,
                http.getHeader("X-Correlation-Id"),
                "application-" + app.getId()));
    }
}