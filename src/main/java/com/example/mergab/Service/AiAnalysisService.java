
package com.example.mergab.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AiAnalysisService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> analyze(
            Integer simulationId,
            Integer zoneId,
            Integer capacity,
            Integer predictedOccupancy,
            String predictedCongestionLevel,
            Integer horizonMinutes) {

        Map<String, Object> request = Map.of(
                "simulationId", simulationId,
                "zoneId", zoneId,
                "capacity", capacity,
                "predictedOccupancy", predictedOccupancy,
                "predictedCongestionLevel", predictedCongestionLevel,
                "horizonMinutes", horizonMinutes
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        Map<String, Object> result;

        try {
            result = restTemplate.postForObject(
                    "http://127.0.0.1:8001/analyze",
                    entity,
                    Map.class
            );

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis service is unavailable or timed out"
            );

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to communicate with AI analysis service"
            );
        }

        if (result == null
                || !(result.get("analysis") instanceof String analysis)
                || analysis.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response from AI analysis service"
            );
        }

        return result;
    }
}
