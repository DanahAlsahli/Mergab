package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.HistoricalAttendance;
import com.example.mergab.Repository.HistoricalAttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoricalAttendanceService {

    private final HistoricalAttendanceRepository historicalAttendanceRepository;

    public List<HistoricalAttendance> getAllAttendance() {
        return historicalAttendanceRepository.findAll();
    }

    public void addAttendance(HistoricalAttendance attendance) {
        HistoricalAttendance duplicate = historicalAttendanceRepository
                .findHistoricalAttendanceByExpoNameAndAttendanceDate(attendance.getExpoName(), attendance.getAttendanceDate());
        if (duplicate != null) {
            throw new ApiException("Attendance for this expo on this date already exists");
        }
        historicalAttendanceRepository.save(attendance);
    }

    public void updateAttendance(Integer id, HistoricalAttendance newAttendance) {
        HistoricalAttendance oldAttendance = historicalAttendanceRepository.findHistoricalAttendanceById(id);
        if (oldAttendance == null) {
            throw new ApiException("Historical attendance not found");
        }

        HistoricalAttendance duplicate = historicalAttendanceRepository
                .findHistoricalAttendanceByExpoNameAndAttendanceDate(newAttendance.getExpoName(), newAttendance.getAttendanceDate());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new ApiException("Attendance for this expo on this date already exists");
        }

        oldAttendance.setExpoName(newAttendance.getExpoName());
        oldAttendance.setAttendanceDate(newAttendance.getAttendanceDate());
        oldAttendance.setTotalAttendance(newAttendance.getTotalAttendance());
        oldAttendance.setSourceUrl(newAttendance.getSourceUrl());

        historicalAttendanceRepository.save(oldAttendance);
    }

    public void deleteAttendance(Integer id) {
        HistoricalAttendance attendance = historicalAttendanceRepository.findHistoricalAttendanceById(id);
        if (attendance == null) {
            throw new ApiException("Historical attendance not found");
        }
        historicalAttendanceRepository.delete(attendance);
    }
}
