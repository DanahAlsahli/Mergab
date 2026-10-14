package com.example.mergab.Model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// سجل التنبيهات: عشان تنبيه الأذان يطلع للزائر مرة وحدة بس لكل صلاة في اليوم
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"visitor_id", "prayer_date", "prayer_key"}))
public class PrayerAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Column(name = "prayer_date", nullable = false)
    private LocalDate prayerDate;

    // fajr, dhuhr, asr, maghrib, isha
    @Column(name = "prayer_key", columnDefinition = "varchar(10) not null")
    private String prayerKey;

    @Column(nullable = false)
    private LocalDateTime seenAt;
}
