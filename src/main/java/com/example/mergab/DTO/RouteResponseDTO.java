
package com.example.mergab.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDTO {

    private String status;
    private String message;
    private Double estimatedCost;
    private String congestionLevel;
    private Integer snapshotMinute;

    private List<ZoneStep> recommendedPath = new ArrayList<>();
    private List<ZoneStep> alternativePath = new ArrayList<>();

    private String destinationWarning;

    private ZoneStep alternativeDestination;
    private List<ZoneStep> alternativeDestinationPath = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ZoneStep {

        private Integer zoneId;
        private String zoneName;
        private Integer occupancy;
        private Integer capacity;
        private Double occupancyRate;
        private String congestionLevel;
    }
}
