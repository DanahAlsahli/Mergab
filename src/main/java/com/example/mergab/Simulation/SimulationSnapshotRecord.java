package com.example.mergab.Simulation;

// One complete record of a zone's state at a specific minute.
public record SimulationSnapshotRecord(
        int minute,
        int zoneId,
        int occupancy,
        int queueLength
) {

    /*
     * minute tells us when the observation happened.
     * zoneId tells us which zone we're observing.
     * occupancy tells us how many visitors are inside the zone.
     * queueLength tells us how many visitors are waiting to enter.
     */
}

