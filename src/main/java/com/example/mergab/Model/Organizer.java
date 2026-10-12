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
public class Organizer {

    @Id
    private Integer id;

    @Column(columnDefinition = "varchar(10) not null unique")
    private String phoneNumber;

    @OneToOne
    @MapsId
    @JsonIgnore
    private User user;

    // الفعاليات والمحاكاة اللي أنشأها (للتتبع فقط - أي منظم يقدر يعدّلها)
    // بدون cascade: حذف المنظم ما يحذف شغله، لأنه شغل الإكسبو كامل
    @OneToMany(mappedBy = "organizer")
    @JsonIgnore
    private Set<ExpoEvent> expoEvents;

    @OneToMany(mappedBy = "organizer")
    @JsonIgnore
    private Set<Simulation> simulations;
}
