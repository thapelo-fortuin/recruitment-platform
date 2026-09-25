package com.recruitplatform.soc_service.repository;

import com.recruitplatform.soc_service.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, String> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    // used for the cooldown check, so we don't raise the same alert every few seconds
    boolean existsByRuleNameAndAffectedUserAndCreatedAtAfter(
            String ruleName, String affectedUser, LocalDateTime after);

    boolean existsByRuleNameAndAffectedServiceAndCreatedAtAfter(
            String ruleName, String affectedService, LocalDateTime after);

    boolean existsByRuleNameAndAffectedEndpointAndCreatedAtAfter(
            String ruleName, String affectedEndpoint, LocalDateTime after);
}