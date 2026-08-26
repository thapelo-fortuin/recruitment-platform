package com.recruitplatform.job_service.repository;
import com.recruitplatform.job_service.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
public interface JobRepository extends JpaRepository<Job, Long> {
    java.util.List<Job> findByStatus(String status);
}