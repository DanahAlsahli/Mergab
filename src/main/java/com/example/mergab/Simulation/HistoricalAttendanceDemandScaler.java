package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.HistoricalAttendance;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses relative daily attendance (not raw Osaka visitor counts) to gently
 * adjust event demand without changing zone capacity or initial occupancy.
 */
public final class HistoricalAttendanceDemandScaler {
    private HistoricalAttendanceDemandScaler() {}

    public static double multiplier(HistoricalAttendance selected, List<HistoricalAttendance> sameExpoHistory) {
        if (selected == null || selected.getTotalAttendance() == null
                || sameExpoHistory == null || sameExpoHistory.size() < 2) {
            return 1.0;
        }
        List<HistoricalAttendance> valid = sameExpoHistory.stream()
                .filter(day -> day != null && day.getTotalAttendance() != null
                        && day.getTotalAttendance() >= 0)
                .toList();
        if (valid.size() < 2) return 1.0;
        double average = valid.stream().mapToDouble(HistoricalAttendance::getTotalAttendance).average().orElse(0);
        if (average <= 0) return 1.0;
        double ratio = selected.getTotalAttendance() / average;
        return Math.max(0.75, Math.min(1.25, ratio));
    }

    /** Makes detached event copies: never edits the persisted expectedDemand. */
    public static List<ExpoEvent> scaledEvents(List<ExpoEvent> events, double multiplier) {
        if (multiplier == 1.0) return events;
        List<ExpoEvent> adjusted = new ArrayList<>(events.size());
        for (ExpoEvent original : events) {
            ExpoEvent copy = new ExpoEvent();
            copy.setId(original.getId());
            copy.setName(original.getName());
            copy.setStartDateTime(original.getStartDateTime());
            copy.setEndDateTime(original.getEndDateTime());
            copy.setExpectedDemand((int) Math.round(original.getExpectedDemand() * multiplier));
            copy.setZone(original.getZone());
            copy.setOrganizer(original.getOrganizer());
            adjusted.add(copy);
        }
        return adjusted;
    }
}
