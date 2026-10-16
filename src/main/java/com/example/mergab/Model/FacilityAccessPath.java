package com.example.mergab.Model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacilityAccessPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Coordinates from walking path to facility entrance
    @Column(columnDefinition = "TEXT", nullable = false)
    private String pathPoints;

    // TO_FACILITY, FROM_FACILITY, BOTH
    @Column(nullable = false, length = 20)
    private String direction = "BOTH";

    // Enable or disable facility access
    @Column(nullable = false)
    private Boolean active = true;

    // Facility with multiple access paths
    @ManyToOne(optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    @JsonIgnore
    private Facility facility;

    // Connects facility to an internal walking path
    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_walk_path_id", nullable = false)
    @JsonIgnore
    private ZoneWalkPath zoneWalkPath;
}
