package com.todongsan.battle_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class InsightClient {

    private final RestTemplate restTemplate;

    @Value("${external.insight.url}")
    private String insightUrl;

    public void triggerAiReport(Long battleId) {
        String url = insightUrl + "/internal/api/v1/insights/battles/" + battleId + "/report";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForObject(url, new HttpEntity<>(null, headers), Void.class);
        } catch (RestClientException e) {
            log.warn("Insight AI report trigger failed: battleId={}, error={}", battleId, e.getMessage());
        }
    }
}
