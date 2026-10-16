
package com.example.mergab.Service;

import com.example.mergab.Model.SimulationSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class AiPredictionService {

    private final RestTemplate restTemplate;

    public AiPredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> predict(
            Integer simulationId,
            Integer zoneId,
            Integer capacity,
            List<SimulationSnapshot> snapshots) {

        List<Map<String, Object>> recentSnapshots =
                snapshots.stream()
                        .map(snapshot -> Map.<String, Object>of(
                                "minute", snapshot.getMinute(),
                                "occupancy", snapshot.getOccupancy(),
                                "queueLength", snapshot.getQueueLength()
                        ))
                        .toList();

        Map<String, Object> request = Map.of(
                "simulationId", simulationId,
                "zoneId", zoneId,
                "capacity", capacity,
                "predictionHorizonMinutes", 10,
                "recentSnapshots", recentSnapshots
        );

        Map<String, Object> result;

        try {
            result = restTemplate.postForObject(
                    "http://127.0.0.1:8001/predict",
                    request,
                    Map.class
            );

        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI prediction service is unavailable or timed out"
            );

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to communicate with AI prediction service"
            );
        }

        if (result == null
                || !result.containsKey("predictedOccupancy")
                || !result.containsKey("predictedCongestionLevel")
                || !result.containsKey("horizonMinutes")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Invalid response from AI prediction service"
            );
        }

        return result;
    }
}
