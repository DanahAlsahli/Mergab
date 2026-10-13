package com.example.mergab.Model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    // Null means the visitor is still inside the zone
    @Column(columnDefinition = "datetime null")
    private LocalDateTime checkOutTime;

    @Column(columnDefinition = "boolean not null default false")
    private Boolean manualCheckOut = false;

    @ManyToOne
    @JoinColumn(name = "visitor_id", nullable = false)
    @JsonIgnore
    private Visitor visitor;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    @JsonIgnore
    private Zone zone;
}