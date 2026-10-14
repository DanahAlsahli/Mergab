package com.example.mergab.Simulation;

import java.util.Map;

/**
 * What does it represent?
 * - occupancy: the number of visitors physically inside each zone.
 * - queueLengths: the number of visitors waiting at each zone's entrance.
 * - Both maps use the zone ID as their key.
 */


// Holds the changing state of the simulation at a specific moment.
public record SimulationState(
        Map<Integer, Integer> occupancy,
        Map<Integer, Integer> queueLengths
) {

    /*
    Map.copyOf(...) creates an unmodifiable copy of each map. This means:
    - Changing the original map won't change the stored state.
    - Other code can't accidentally add or remove entries through the record's accessors.
    - Each state can safely represent one moment in the simulation.
     */

    // Keep a separate, unmodifiable copy of each map.
    public SimulationState {
        occupancy = Map.copyOf(occupancy);
        queueLengths = Map.copyOf(queueLengths);
    }

}
