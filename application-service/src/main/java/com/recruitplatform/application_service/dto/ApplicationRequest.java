package com.recruitplatform.application_service.dto;
import jakarta.validation.constraints.NotNull;
public class ApplicationRequest {
    @NotNull private Long jobId;
    public Long getJobId() {
        return jobId;
    }
    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}