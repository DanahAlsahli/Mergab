package com.example.mergab.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Simulation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 600, message = "Duration cannot exceed 600 minutes")
    @Column(columnDefinition = "int not null")
    private Integer durationMinutes;

    @Pattern(regexp = "^(PENDING|RUNNING|COMPLETED|FAILED)$",
            message = "Status must be PENDING, RUNNING, COMPLETED or FAILED")
    @Column(columnDefinition = "varchar(20) not null default 'PENDING' check (status in ('PENDING','RUNNING','COMPLETED','FAILED'))")
    private String status = "PENDING";

    @NotNull(message = "Random seed is required")
    @Column(columnDefinition = "bigint not null")
    private Long randomSeed;

    @Column(columnDefinition = "datetime")
    private LocalDateTime startDateTime;

    @Column(columnDefinition = "datetime")
    private LocalDateTime endDateTime;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Intervention> interventions;

    @OneToMany(mappedBy = "simulation")
    @JsonIgnore
    private Set<SimulationSnapshot> snapshots;

    @ManyToOne
    @JoinColumn(name = "historical_attendance_id", referencedColumnName = "id", nullable = false)
    @JsonIgnore
    private HistoricalAttendance historicalAttendance;
}
