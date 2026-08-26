package com.recruitplatform.job_service.model;
import jakarta.persistence.*; @Entity @Table(name = "jobs")
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; @Column(nullable = false)
    private String title; @Column(nullable = false)
    private String department; @Column(columnDefinition = "TEXT")
    private String description; @Column(nullable = false)
    private String requiredSkills; @Column(nullable = false)
    private String status; // "OPEN" or "CLOSED" @Column(nullable = false)
    private String postedByEmail; // which recruiter posted it
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPostedByEmail() { return postedByEmail; }
    public void setPostedByEmail(String postedByEmail) { this.postedByEmail = postedByEmail; }
}