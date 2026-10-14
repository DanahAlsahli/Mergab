package com.example.mergab.Repository;

import com.example.mergab.Model.PrayerAlertLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PrayerAlertLogRepository extends JpaRepository<PrayerAlertLog, Integer> {

    boolean existsByVisitor_IdAndPrayerDateAndPrayerKey(Integer visitorId, LocalDate prayerDate, String prayerKey);
}
