
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
             * If the simulation starts after the arrival window has opened,
             * the arrivals that would have occurred before the simulation began
             * are redistributed over the remaining pre-event minutes. Otherwise
             * demand was silently lost whenever a simulation started late.
             * If the event is beyond the simulation horizon, only slots actually
             * inside the horizon are scheduled, preserving the original share.
             */
            LocalDateTime firstEligible = windowStart.isBefore(timelineStart)
                    ? timelineStart : windowStart;
            LocalDateTime lastEligibleExclusive = eventStart.isBefore(timelineEnd)
                    ? eventStart : timelineEnd;
            if (!firstEligible.isBefore(lastEligibleExclusive)) {
                // If the simulation begins after the entire pre-event window and
                // the event is already starting/active, there are no future
                // pre-event slots left. Admit the unrepresented expected demand
                // in the first simulated minute instead of silently dropping it.
                if (windowStart.isBefore(timelineStart)
                        && !eventStart.isAfter(timelineStart)
                        && event.getEndDateTime().isAfter(timelineStart)) {
                    arrivals.computeIfAbsent(1, key -> new HashMap<>())
                            .merge(zoneId, demand, Integer::sum);
                }
                continue;
            }

            int eligibleSlots = (int) Duration.between(
                    firstEligible, lastEligibleExclusive).toMinutes();
            boolean windowAlreadyStarted = windowStart.isBefore(timelineStart);
            int allocationSlots = windowAlreadyStarted
                    ? eligibleSlots : arrivalWindowMinutes;

            for (int slot = 0; slot < eligibleSlots; slot++) {
                LocalDateTime slotTime = firstEligible.plusMinutes(slot);
                int allocationSlot = windowAlreadyStarted
                        ? slot
                        : (int) Duration.between(windowStart, slotTime).toMinutes();

                long previousCumulative =
                        ((long) allocationSlot * demand) / allocationSlots;
                long nextCumulative =
                        ((long) (allocationSlot + 1) * demand) / allocationSlots;
                int visitorsThisSlot = (int) (nextCumulative - previousCumulative);
                if (visitorsThisSlot == 0) continue;

                // A slot at the simulation start belongs to minute one.
                int simulationMinute = (int) Duration.between(
                        timelineStart, slotTime).toMinutes() + 1;
                arrivals.computeIfAbsent(simulationMinute, key -> new HashMap<>())
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
