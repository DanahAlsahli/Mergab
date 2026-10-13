package com.example.mergab.Repository;

import com.example.mergab.Model.ExpoEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface ExpoEventRepository extends JpaRepository<ExpoEvent,Integer> {
    ExpoEvent findExpoEventById(Integer id);
    List<ExpoEvent> findByZone_Id(Integer zoneId);
    List<ExpoEvent> findExpoEventsByOrganizer_Id(Integer organizerId);

    List<ExpoEvent> findByEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime now);
    List<ExpoEvent> findByStartDateTimeBeforeAndEndDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime dayEnd, LocalDateTime dayStart);

}
