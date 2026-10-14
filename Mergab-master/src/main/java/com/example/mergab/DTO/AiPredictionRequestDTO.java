package com.example.mergab.DTO;

import java.util.List;

public record AiPredictionRequestDTO(
        Integer simulationId,
        Integer zoneId,
        Integer capacity,
        Integer predictionHorizonMinutes,
        List<SnapshotInput> recentSnapshots
) {
    public record SnapshotInput(
            Integer minute,
            Integer occupancy,
            Integer queueLength
    ) {}
}

