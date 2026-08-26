package com.recruitplatform.application_service.model;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name = "applications")
public class Application {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; @Column(nullable = false)
    private String candidateEmail; @Column(nullable = false)
    private Long jobId; @Column(nullable = false)
    private String status; // APPLIED, SCREENING, INTERVIEW, OFFER, HIRED, REJECTED @Column(nullable = false)
    private LocalDateTime appliedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCandidateEmail() { return candidateEmail; }
    public void setCandidateEmail(String candidateEmail) { this.candidateEmail = candidateEmail; }
    public Long getJobId() { return jobId; }
    public void setJobId(Long jobId) { this.jobId = jobId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}