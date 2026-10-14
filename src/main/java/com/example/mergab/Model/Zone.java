package com.example.mergab.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Zone name is required")
    @Column(columnDefinition = "varchar(50) not null unique")
    private String name;

    @NotNull(message = "Capacity is required")
    @Positive(message = "Capacity must be greater than zero")
    @Column(columnDefinition = "int not null")
    private Integer capacity;

    @NotNull(message = "Initial visitors is required")
    @PositiveOrZero(message = "Initial visitors cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer initialVisitors;

    private Boolean hasPrayerArea = false;

    @Column(columnDefinition = "boolean not null default false")
    private Boolean indoor = false;

    @Column(nullable = true)
    private Double mapX;

    @Column(nullable = true)
    private Double mapY;

    @OneToMany(mappedBy = "fromZone")
    @JsonIgnore
    private Set<ZoneConnection> outgoingConnections;


    @OneToMany(mappedBy = "toZone")
    @JsonIgnore
    private Set<ZoneConnection> incomingConnections;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private Set<ExpoEvent> events;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private Set<SimulationSnapshot> snapshots;

    @OneToMany(mappedBy = "zone")
    @JsonIgnore
    private Set<ZoneVisit> zoneVisits;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private Set<Facility> facilities;
}
