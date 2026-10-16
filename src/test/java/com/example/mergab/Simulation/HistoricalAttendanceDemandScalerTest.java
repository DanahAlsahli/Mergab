package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.HistoricalAttendance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricalAttendanceDemandScalerTest {
    private HistoricalAttendance day(int attendance) {
        HistoricalAttendance day = new HistoricalAttendance();
        day.setExpoName("Expo 2025 Osaka");
        day.setTotalAttendance(attendance);
        return day;
    }

    @Test
    void selectedHigherAttendanceIncreasesOnlyEventDemand() {
        HistoricalAttendance high = day(120_000);
        HistoricalAttendance low = day(80_000);
        double factor = HistoricalAttendanceDemandScaler.multiplier(high, List.of(high, low));
        assertEquals(1.2, factor, 0.00001);
        ExpoEvent event = new ExpoEvent();
        event.setExpectedDemand(60);
        List<ExpoEvent> scaled = HistoricalAttendanceDemandScaler.scaledEvents(List.of(event), factor);
        assertEquals(72, scaled.get(0).getExpectedDemand());
        assertEquals(60, event.getExpectedDemand(), "Must not mutate the database event entity");
    }

    @Test
    void lowerAttendanceReducesDemandAndRatioIsBounded() {
        HistoricalAttendance low = day(20_000);
        HistoricalAttendance high = day(180_000);
        assertEquals(0.75, HistoricalAttendanceDemandScaler.multiplier(low, List.of(low, high)), 0.00001);
        assertEquals(1.25, HistoricalAttendanceDemandScaler.multiplier(high, List.of(low, high)), 0.00001);
    }

    @Test
    void singleHistoricalDayLeavesDemandAlone() {
        HistoricalAttendance only = day(100_000);
        assertEquals(1.0, HistoricalAttendanceDemandScaler.multiplier(only, List.of(only)));
    }
}
