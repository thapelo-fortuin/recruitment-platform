package com.recruitplatform.application_service.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import java.util.Map;
@Component public class JobServiceClient {
    private final RestClient restClient;
    public JobServiceClient(@Value("${job-service.url}") String jobServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(jobServiceUrl).build();
    }
    public Map<String, Object> getJob(Long jobId, String authHeader) {
        try { return restClient.get() .uri("/api/v1/jobs/{id}", jobId) .header("Authorization", authHeader) .retrieve() .body(Map.class);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
