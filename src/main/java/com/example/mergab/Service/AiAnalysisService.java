
package com.example.mergab.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AiAnalysisService {

    private static final Logger logger =
            LoggerFactory.getLogger(AiAnalysisService.class);

    private final RestTemplate restTemplate;
    private final String analyzeUrl;

    public AiAnalysisService(
            @Value("${mirqab.ai.base-url:http://127.0.0.1:8001}") String baseUrl,
            @Value("${mirqab.ai.analyze-path:/analyze}") String analyzePath) {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000);

        this.restTemplate = new RestTemplate(factory);

        this.analyzeUrl = baseUrl.replaceAll("/+$", "")
                + "/"
                + analyzePath.replaceAll("^/+", "");
    }

    public Map<String, Object> analyze(
            Integer simulationId,
            Integer zoneId,
            Integer capacity,
            Integer predictedOccupancy,
            String predictedCongestionLevel,
            Integer horizonMinutes) {

        if (simulationId == null || simulationId <= 0
                || zoneId == null || zoneId <= 0
                || capacity == null || capacity <= 0
                || predictedOccupancy == null || predictedOccupancy < 0
                || predictedCongestionLevel == null
                || !predictedCongestionLevel.matches("LOW|MEDIUM|HIGH")
                || horizonMinutes == null || horizonMinutes <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid AI analysis request data"
            );
        }

        Map<String, Object> request = Map.of(
                "simulationId", simulationId,
                "zoneId", zoneId,
                "capacity", capacity,
                "predictedOccupancy", predictedOccupancy,
                "predictedCongestionLevel", predictedCongestionLevel,
                "horizonMinutes", horizonMinutes
        );

        try {
            Map<?, ?> response = restTemplate.postForObject(
                    analyzeUrl, request, Map.class
            );

            if (response == null
                    || !(response.get("analysis") instanceof String analysis)
                    || analysis.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI analysis service returned an invalid response"
                );
            }

            return Map.of(
                    "simulationId", simulationId,
                    "zoneId", zoneId,
                    "analysis", analysis
            );

        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            logger.error("AI request failed at {}", analyzeUrl, e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI analysis request failed; check application logs"
            );
        }
    }
}
