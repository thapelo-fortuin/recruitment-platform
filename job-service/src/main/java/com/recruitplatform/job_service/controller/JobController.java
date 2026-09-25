package com.recruitplatform.job_service.controller;

import com.recruitplatform.job_service.dto.JobRequest;
import com.recruitplatform.job_service.model.Job;
import com.recruitplatform.job_service.repository.JobRepository;
import com.recruitplatform.job_service.soc.SocEvent;
import com.recruitplatform.job_service.soc.SocEventClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final SocEventClient socClient;

    public JobController(JobRepository jobRepository, SocEventClient socClient) {
        this.jobRepository = jobRepository;
        this.socClient = socClient;
    }

    @PostMapping
    public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request,
                                       Authentication authentication,
                                       HttpServletRequest httpRequest) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDepartment(request.getDepartment());
        job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills());
        job.setStatus("OPEN");
        job.setPostedByEmail(authentication.getName()); // the email set by JwtAuthenticationFilter
        jobRepository.save(job);

        reportAction(httpRequest, authentication, "Job created", job);
        return ResponseEntity.ok(job);
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Long id) {
        Optional<Job> job = jobRepository.findById(id);
        return job.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeJob(@PathVariable Long id,
                                      Authentication authentication,
                                      HttpServletRequest httpRequest) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Job job = jobOpt.get();
        job.setStatus("CLOSED");
        jobRepository.save(job);

        reportAction(httpRequest, authentication, "Job closed", job);
        return ResponseEntity.ok(job);
    }

    private void reportAction(HttpServletRequest http, Authentication auth, String message, Job job) {
        String ip = http.getHeader("X-Client-Ip");
        if (ip == null || ip.isBlank()) {
            ip = http.getRemoteAddr();
        }
        socClient.send(new SocEvent(
                null, "job-service", "BUSINESS_ACTION", "INFO",
                auth != null ? auth.getName() : null,
                ip, http.getRequestURI(), http.getMethod(), 200,
                message + " (job " + job.getId() + ")",
                http.getHeader("X-Correlation-Id"),
                "job-" + job.getId()));
    }
}