package com.example.mergab.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExpoEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Event name is required")
    @Column(columnDefinition = "varchar(100) not null")
    private String name;

    @NotNull(message = "Event start date and time is required")
    @Column(columnDefinition = "datetime not null")
    private LocalDateTime startDateTime;

    @NotNull(message = "Event end date and time is required")
    @Column(columnDefinition = "datetime not null")
    private LocalDateTime endDateTime;

    @NotNull(message = "Expected demand is required")
    @PositiveOrZero(message = "Expected demand cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer expectedDemand;

    @ManyToOne
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;
}