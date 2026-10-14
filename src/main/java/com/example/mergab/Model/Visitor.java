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
public class Visitor {

    @Id
    private Integer id;

    @Column(columnDefinition = "varchar(10) not null unique")
    private String phoneNumber;

    @Column(nullable = false, unique = true, updatable = false)
    private String qrToken;

    @OneToOne
    @MapsId
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "visitor")
    @JsonIgnore
    private Set<ZoneVisit> zoneVisits;

    // الزائر يختار يشوف أوقات الصلاة أو لا
    @Column(columnDefinition = "boolean not null default false")
    private Boolean showPrayerTimes = false;
}
