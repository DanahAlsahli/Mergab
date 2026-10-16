package com.example.mergab.Model;



import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "varchar(100) not null")
    private String name;

    @Column(columnDefinition = "varchar(20) not null")
    private String type; // RESTROOM, PRAYER_ROOM, EXIT, ENTRANCE

    @Column(columnDefinition = "varchar(10) not null")
    private String gender; // MALE, FEMALE, BOTH


    // Geographic coordinates for real GPS
    @Column(nullable = true)
    private Double latitude;

    @Column(nullable = true)
    private Double longitude;

    // Position on the Expo map (0–100%)
    @Column(nullable = true)
    private Double mapX;

    @Column(nullable = true)
    private Double mapY;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    @JsonIgnore
    private Zone zone;


    @OneToMany(mappedBy = "facility")
    @JsonIgnore
    private Set<FacilityAccessPath> accessPaths;

}
