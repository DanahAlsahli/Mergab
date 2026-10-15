package com.example.mergab.Model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ZoneConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;



    @NotNull(message = "Maximum visitors per minute is required")
    @Positive(message = "Maximum visitors must be greater than zero")
    @Column(columnDefinition = "int not null")
    private Integer maxVisitorsPerMinute;

    @Column(columnDefinition = "TEXT")
    private String pathPoints;

    @ManyToOne
    @JoinColumn(name = "from_zone_id", nullable = false)
    private Zone fromZone;

    @ManyToOne
    @JoinColumn(name = "to_zone_id", nullable = false)
    private Zone toZone;
}