package com.example.mergab.Model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneWalkPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Walking path name
    @Column(nullable = false, length = 100)
    private String name;

    // Path coordinates on the map
    @Column(columnDefinition = "TEXT", nullable = false)
    private String pathPoints;

    // BOTH, FORWARD, REVERSE
    @Column(nullable = false, length = 20)
    private String direction = "BOTH";

    // Organizer can enable or disable the path
    @Column(nullable = false)
    private Boolean active = true;

    // Walking path belongs to one zone
    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    @JsonIgnore
    private Zone zone;
}
