package com.recruitplatform.job_service.controller;
import com.recruitplatform.job_service.dto.JobRequest;
import com.recruitplatform.job_service.model.Job;
import com.recruitplatform.job_service.repository.JobRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;
@RestController @RequestMapping("/api/jobs")
public class JobController {
    private final JobRepository jobRepository;
    public JobController(JobRepository jobRepository) { this.jobRepository = jobRepository; }
    @PostMapping public ResponseEntity<?> createJob(@Valid @RequestBody JobRequest request, Authentication authentication) { Job job = new Job(); job.setTitle(request.getTitle()); job.setDepartment(request.getDepartment()); job.setDescription(request.getDescription());
        job.setRequiredSkills(request.getRequiredSkills()); job.setStatus("OPEN"); job.setPostedByEmail(authentication.getName()); // the email set by JwtAuthenticationFilter
        jobRepository.save(job);
        return ResponseEntity.ok(job); }
    @GetMapping public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    @GetMapping("/{id}") public ResponseEntity<?> getJob(@PathVariable Long id) {
        Optional<Job> job = jobRepository.findById(id); return job.<ResponseEntity<?>>map(ResponseEntity::ok) .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PutMapping("/{id}/close") public ResponseEntity<?> closeJob(@PathVariable Long id) {
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) { return ResponseEntity.notFound().build(); } Job job = jobOpt.get(); job.setStatus("CLOSED");
        jobRepository.save(job); return ResponseEntity.ok(job);
    }
}
