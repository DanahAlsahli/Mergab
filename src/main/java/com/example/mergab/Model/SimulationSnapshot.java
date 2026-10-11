package com.example.mergab.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimulationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Minute is required")
    @PositiveOrZero(message = "Minute cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer minute;

    @NotNull(message = "Occupancy is required")
    @PositiveOrZero(message = "Occupancy cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer occupancy;

    @NotNull(message = "Queue length is required")
    @PositiveOrZero(message = "Queue length cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer queueLength;

//    @ManyToOne
//    @JoinColumn(name = "simulation_id", nullable = false)
//    private Simulation simulation;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
}