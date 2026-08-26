package com.recruitplatform.job_service.dto;
import jakarta.validation.constraints.NotBlank;

public class JobRequest {
    @NotBlank private String title;
    @NotBlank private String department;
    private String description; @NotBlank
    private String requiredSkills;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
}
