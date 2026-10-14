package com.example.mergab.Service;

import com.example.mergab.Model.SimulationSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;

@Service
public class AiPredictionService {

    private final RestTemplate restTemplate = new RestTemplate();

    public Map predict(Integer simulationId,
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(request, headers);

        return restTemplate.postForObject(
                "http://127.0.0.1:8000/predict",
                entity,
                Map.class
        );
    }
}
