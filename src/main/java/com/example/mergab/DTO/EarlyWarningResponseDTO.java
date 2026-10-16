package com.example.mergab.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EarlyWarningResponseDTO {

    private Integer simulationId;
    private Integer horizonMinutes;
    private Integer totalZones;
    private Integer analyzedZones;
    private Integer zonesWithoutEnoughData;
    private List<ZoneWarning> warnings;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ZoneWarning {

        private Integer zoneId;
        private String zoneName;

        private Integer currentOccupancy;
        private Integer currentQueueLength;
        private Integer capacity;

        private Integer predictedOccupancy;
        private Double predictedOccupancyPercentage;

        private String congestionLevel;
        private String status;
        private String recommendation;
    }
}
