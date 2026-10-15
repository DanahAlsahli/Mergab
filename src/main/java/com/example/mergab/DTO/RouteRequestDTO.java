
package com.example.mergab.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRequestDTO {

    @NotNull(message = "Current zone ID is required")
    @Positive(message = "Current zone ID must be a positive number")
    private Integer currentZoneId;

    @NotNull(message = "Destination zone ID is required")
    @Positive(message = "Destination zone ID must be a positive number")
    private Integer destinationZoneId;

    @NotNull(message = "Simulation ID is required")
    @Positive(message = "Simulation ID must be a positive number")
    private Integer simulationId;
}
