
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calculates event-driven visitor arrivals independently of the existing
 * CrowdSimulationEngine.
 *
 * The result maps:
 * simulation minute -> zone ID -> number of arriving visitors.
 *
 * Minute 1 represents the first minute after the simulation start.
 * Minute 0 is reserved for the initial simulation state.
 */
@Component
public class EventDrivenSimulationEngine {

    /**
     * Distributes each event's expected demand across a configurable
     * pre-event arrival window.
     *
     * This method schedules arrivals only. It does not change zone occupancy,
     * queues, or corridor movement.
     *
     * @param events events to consider
     * @param simulationStart start of the simulated timeline
     * @param durationMinutes total simulation duration
     * @param arrivalWindowMinutes minutes before each event starts
     * @return arrivals grouped by simulation minute and zone ID
     */
    public Map<Integer, Map<Integer, Integer>> scheduleArrivals(
            List<ExpoEvent> events,
            LocalDateTime simulationStart,
            int durationMinutes,
            int arrivalWindowMinutes) {

        if (events == null) {
            throw new IllegalArgumentException("Events cannot be null");
        }

        if (simulationStart == null) {
            throw new IllegalArgumentException(
                    "Simulation start time cannot be null");
        }

        if (durationMinutes < 1) {
            throw new IllegalArgumentException(
                    "Simulation duration must be at least one minute");
        }

        if (arrivalWindowMinutes < 1) {
            throw new IllegalArgumentException(
                    "Arrival window must be at least one minute");
        }

        LocalDateTime timelineStart =
                simulationStart.truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime timelineEnd =
                timelineStart.plusMinutes(durationMinutes);

        Map<Integer, Map<Integer, Integer>> arrivals = new HashMap<>();

        for (ExpoEvent event : events) {
            validateEvent(event);

            int demand = event.getExpectedDemand();

            if (demand == 0) {
                continue;
            }

            Integer zoneId = event.getZone().getId();

            LocalDateTime windowStart =
                    event.getStartDateTime()
                            .minusMinutes(arrivalWindowMinutes);

            LocalDateTime eventStart = event.getStartDateTime();

            /*
             * Divide the FULL arrival window into minute slots.
             * Calculate each slot's allocation from cumulative demand,
             * so rounding never causes total scheduled demand to exceed
             * expectedDemand.
             */
            for (int slot = 0; slot < arrivalWindowMinutes; slot++) {
                LocalDateTime slotTime = windowStart.plusMinutes(slot);

                // Arrivals are scheduled before event start, never at start.
                if (slotTime.isBefore(timelineStart)
                        || !slotTime.isBefore(timelineEnd)
                        || !slotTime.isBefore(eventStart)) {
                    continue;
                }

                long previousCumulative =
                        ((long) slot * demand) / arrivalWindowMinutes;

                long nextCumulative =
                        ((long) (slot + 1) * demand) / arrivalWindowMinutes;

                int visitorsThisSlot =
                        (int) (nextCumulative - previousCumulative);

                if (visitorsThisSlot == 0) {
                    continue;
                }

                /*
                 * Minute 1 is the first interval after timelineStart.
                 * A slot at timelineStart belongs to minute 1.
                 */
                int simulationMinute =
                        (int) Duration.between(
                                timelineStart, slotTime).toMinutes() + 1;

                arrivals
                        .computeIfAbsent(
                                simulationMinute, key -> new HashMap<>())
                        .merge(zoneId, visitorsThisSlot, Integer::sum);
            }
        }

        return arrivals;
    }

    private void validateEvent(ExpoEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getStartDateTime() == null
                || event.getEndDateTime() == null) {
            throw new IllegalArgumentException(
                    "Event start and end times are required");
        }

        if (!event.getEndDateTime().isAfter(event.getStartDateTime())) {
            throw new IllegalArgumentException(
                    "Event end time must be after its start time");
        }

        if (event.getExpectedDemand() == null
                || event.getExpectedDemand() < 0) {
            throw new IllegalArgumentException(
                    "Event expected demand cannot be negative");
        }

        if (event.getZone() == null || event.getZone().getId() == null) {
            throw new IllegalArgumentException(
                    "Event must have a zone with an ID");
        }
    }
}
