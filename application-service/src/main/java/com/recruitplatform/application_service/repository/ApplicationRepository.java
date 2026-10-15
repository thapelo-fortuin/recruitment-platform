package com.recruitplatform.application_service.repository;
import com.recruitplatform.application_service.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByCandidateEmail(String candidateEmail);
    List<Application> findByJobId(Long jobId);
    List<Application> findByJobIdIn(List<Long> jobIds);
    boolean existsByCandidateEmailAndJobId(String candidateEmail, Long jobId);
}
