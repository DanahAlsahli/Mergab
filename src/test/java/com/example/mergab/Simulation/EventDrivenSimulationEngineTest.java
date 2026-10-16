
package com.example.mergab.Simulation;

import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Zone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventDrivenSimulationEngineTest {

    private EventDrivenSimulationEngine engine;
    private LocalDateTime simulationStart;
    private Zone zone;

    @BeforeEach
    void setUp() {
        engine = new EventDrivenSimulationEngine();
        simulationStart = LocalDateTime.of(2026, 10, 16, 10, 0);

        zone = new Zone();
        zone.setId(1);
        zone.setName("Exhibition Hall");
        zone.setCapacity(100);
        zone.setInitialVisitors(0);
    }

    private ExpoEvent createEvent(
            int zoneId,
            int demand,
            LocalDateTime start,
            LocalDateTime end) {

        Zone eventZone = new Zone();
        eventZone.setId(zoneId);
        eventZone.setName("Zone " + zoneId);
        eventZone.setCapacity(100);
        eventZone.setInitialVisitors(0);

        ExpoEvent event = new ExpoEvent();
        event.setId(1);
        event.setName("Test Event");
        event.setZone(eventZone);
        event.setExpectedDemand(demand);
        event.setStartDateTime(start);
        event.setEndDateTime(end);

        return event;
    }

    @Test
    void shouldDistributeDemandAcrossArrivalWindow() {
        ExpoEvent event = createEvent(
                1,
                10,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(60)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(event), simulationStart, 15, 5);

        // The five slots are minutes 6 through 10.
        for (int minute = 6; minute <= 10; minute++) {
            assertEquals(
                    2,
                    arrivals.get(minute).get(1),
                    "Expected two arrivals at minute " + minute
            );
        }

        int totalArrivals = arrivals.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(1, 0))
                .sum();

        assertEquals(10, totalArrivals);
    }

    @Test
    void shouldNotScheduleArrivalsAtOrAfterEventStart() {
        ExpoEvent event = createEvent(
                1,
                10,
                simulationStart.plusMinutes(5),
                simulationStart.plusMinutes(30)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(event), simulationStart, 10, 5);

        assertTrue(arrivals.keySet().stream()
                .allMatch(minute -> minute < 6));

        assertEquals(
                10,
                arrivals.values().stream()
                        .mapToInt(byZone -> byZone.getOrDefault(1, 0))
                        .sum()
        );
    }

    @Test
    void shouldCombineArrivalsFromMultipleEventsInSameZone() {
        ExpoEvent firstEvent = createEvent(
                1, 10,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        ExpoEvent secondEvent = createEvent(
                1, 5,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(firstEvent, secondEvent),
                        simulationStart, 15, 5);

        int totalArrivals = arrivals.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(1, 0))
                .sum();

        assertEquals(15, totalArrivals);
    }

    @Test
    void shouldKeepArrivalsSeparatedByZone() {
        ExpoEvent firstEvent = createEvent(
                1, 10,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        ExpoEvent secondEvent = createEvent(
                2, 5,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(firstEvent, secondEvent),
                        simulationStart, 15, 5);

        int zoneOneTotal = arrivals.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(1, 0))
                .sum();

        int zoneTwoTotal = arrivals.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(2, 0))
                .sum();

        assertEquals(10, zoneOneTotal);
        assertEquals(5, zoneTwoTotal);
    }

    @Test
    void shouldReturnNoArrivalsForZeroDemand() {
        ExpoEvent event = createEvent(
                1, 0,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(event), simulationStart, 15, 5);

        assertTrue(arrivals.isEmpty());
    }

    @Test
    void shouldIgnoreEventsOutsideSimulationWindow() {
        ExpoEvent event = createEvent(
                1, 10,
                simulationStart.plusMinutes(30),
                simulationStart.plusMinutes(60)
        );

        Map<Integer, Map<Integer, Integer>> arrivals =
                engine.scheduleArrivals(
                        List.of(event), simulationStart, 15, 5);

        assertTrue(arrivals.isEmpty());
    }

    @Test
    void shouldRejectNullEventList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.scheduleArrivals(
                        null, simulationStart, 15, 5)
        );
    }

    @Test
    void shouldRejectInvalidSimulationDuration() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.scheduleArrivals(
                        List.of(), simulationStart, 0, 5)
        );
    }

    @Test
    void shouldRejectInvalidArrivalWindow() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.scheduleArrivals(
                        List.of(), simulationStart, 15, 0)
        );
    }

    @Test
    void shouldRejectEventWithNegativeDemand() {
        ExpoEvent event = createEvent(
                1, -1,
                simulationStart.plusMinutes(10),
                simulationStart.plusMinutes(30)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.scheduleArrivals(
                        List.of(event), simulationStart, 15, 5)
        );
    }

    @Test
    void shouldRejectEventWithInvalidTimeRange() {
        ExpoEvent event = createEvent(
                1, 10,
                simulationStart.plusMinutes(30),
                simulationStart.plusMinutes(10)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.scheduleArrivals(
                        List.of(event), simulationStart, 45, 5)
        );
    }

    @Test
    void shouldRedistributeMissedArrivalWindowAcrossRemainingMinutesWhenStartedLate() {
        Zone zone = new Zone();
        zone.setId(99);
        zone.setCapacity(100);
        zone.setInitialVisitors(0);
        ExpoEvent event = new ExpoEvent();
        event.setId(99);
        event.setName("Late-start event");
        event.setZone(zone);
        event.setExpectedDemand(10);
        event.setStartDateTime(LocalDateTime.of(2026, 10, 16, 10, 10));
        event.setEndDateTime(LocalDateTime.of(2026, 10, 16, 11, 0));

        Map<Integer, Map<Integer, Integer>> schedule = new EventDrivenSimulationEngine()
                .scheduleArrivals(List.of(event), LocalDateTime.of(2026, 10, 16, 10, 5), 10, 30);

        assertEquals(10, schedule.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(99, 0)).sum());
        assertTrue(schedule.keySet().stream().allMatch(minute -> minute >= 1 && minute <= 5));
    }

    @Test
    void shouldNotDropDemandWhenSimulationStartsAtEventStart() {
        ExpoEvent event = createEvent(7, 12,
                simulationStart, simulationStart.plusMinutes(20));
        Map<Integer, Map<Integer, Integer>> schedule = engine.scheduleArrivals(
                List.of(event), simulationStart, 10, 30);
        // The event fixture targets zone 7, so assertions must inspect zone 7.
        assertEquals(12, schedule.values().stream()
                .mapToInt(byZone -> byZone.getOrDefault(7, 0)).sum());
        assertEquals(12, schedule.get(1).get(7));
    }
}
