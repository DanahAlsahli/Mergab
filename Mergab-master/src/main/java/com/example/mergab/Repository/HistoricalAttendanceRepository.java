package com.example.mergab.Repository;

import com.example.mergab.Model.HistoricalAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoricalAttendanceRepository extends JpaRepository<HistoricalAttendance,Integer> {

    HistoricalAttendance findHistoricalAttendanceById(Integer id);

    // نفس المعرض في نفس اليوم ما يتكرر
    HistoricalAttendance findHistoricalAttendanceByExpoNameAndAttendanceDate(String expoName, LocalDate attendanceDate);

    List<HistoricalAttendance> findByExpoNameOrderByAttendanceDateDesc(String expoName);
}
