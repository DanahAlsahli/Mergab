
package com.example.mergab.Simulation;

// One record of how many visitors are in a zone at a specific minute.
public record OccupancyRecord(
        int minute,
        int zoneId,
        int visitors
) {

    /*
     * minute tells us when the observation happened.
     * zoneId tells us which zone we're observing.
     * visitors tells us how many visitors are there.
     */

}
