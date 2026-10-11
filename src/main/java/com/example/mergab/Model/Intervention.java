package com.example.mergab.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Intervention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Start minute is required")
    @PositiveOrZero(message = "Start minute cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer startMinute;

    @NotNull(message = "Visitors per minute is required")
    @Positive(message = "Visitors per minute must be positive")
    @Column(columnDefinition = "int not null")
    private Integer visitorsPerMinute;

    @ManyToOne
    @JoinColumn(name = "simulation_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private Simulation simulation;

    @ManyToOne
    @JoinColumn(name = "source_zone_id", referencedColumnName = "id", nullable = false)
    private Zone sourceZone;

    @ManyToOne
    @JoinColumn(name = "target_zone_id", referencedColumnName = "id", nullable = false)
    private Zone targetZone;


}
