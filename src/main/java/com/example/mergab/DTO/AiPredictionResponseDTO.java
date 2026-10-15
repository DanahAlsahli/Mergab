package com.example.mergab.DTO;

public record AiPredictionResponseDTO(
        Integer zoneId,
        Integer predictedOccupancy,
        String predictedCongestionLevel,
        Integer horizonMinutes
) {
}
