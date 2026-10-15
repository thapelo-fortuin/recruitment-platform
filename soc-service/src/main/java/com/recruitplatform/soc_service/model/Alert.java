package com.recruitplatform.soc_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    private String alertId;

    @Column(nullable = false)
    private String ruleName;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status;              // OPEN, ACKNOWLEDGED, RESOLVED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private String affectedService;
    private String affectedUser;
    private String affectedIp;
    private String affectedEndpoint;

    @Column(columnDefinition = "TEXT")
    private String relatedEvents;       // comma-separated eventIds, kept simple on purpose

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String recommendedAction;

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getAffectedService() { return affectedService; }
    public void setAffectedService(String affectedService) { this.affectedService = affectedService; }
    public String getAffectedUser() { return affectedUser; }
    public void setAffectedUser(String affectedUser) { this.affectedUser = affectedUser; }
    public String getAffectedIp() { return affectedIp; }
    public void setAffectedIp(String affectedIp) { this.affectedIp = affectedIp; }
    public String getAffectedEndpoint() { return affectedEndpoint; }
    public void setAffectedEndpoint(String affectedEndpoint) { this.affectedEndpoint = affectedEndpoint; }
    public String getRelatedEvents() { return relatedEvents; }
    public void setRelatedEvents(String relatedEvents) { this.relatedEvents = relatedEvents; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}