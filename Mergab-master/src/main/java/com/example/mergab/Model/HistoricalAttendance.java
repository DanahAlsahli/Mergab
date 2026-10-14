package com.example.mergab.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;



@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"expo_name", "attendance_date"}))

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotEmpty(message = "Expo name is required")
    @Column(columnDefinition = "varchar(100) not null")
    private String expoName;

    @NotNull(message = "Attendance date is required")
    @PastOrPresent(message = "Attendance date cannot be in the future")
    @Column(columnDefinition = "date not null")
    private LocalDate attendanceDate;

    @NotNull(message = "Total attendance is required")
    @PositiveOrZero(message = "Total attendance cannot be negative")
    @Column(columnDefinition = "int not null")
    private Integer totalAttendance;

    @NotEmpty(message = "Source URL is required")
    @Column(columnDefinition = "varchar(500) not null")
    private String sourceUrl;

    // Historical attendance should not delete its simulations
    @OneToMany(mappedBy = "historicalAttendance")
    @JsonIgnore
    private Set<Simulation> simulations;
}
