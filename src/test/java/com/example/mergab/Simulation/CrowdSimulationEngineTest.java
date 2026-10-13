package com.example.mergab.Simulation;


import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// for testing the simulation calculation and making sure everything is ok


public class CrowdSimulationEngineTest {

//    // this thing to verify this as a test
//    @Test
//    void firstTestShouldPass() {
//        // check if the condition pass
//        assertTrue(true);
//    } // if this runs successfully then our basic testing is working


    // test simulation on 2 Zones A and B
    // move 10 visitors every 1 minutes from A to B
    @Test
    void shouldMoveTenVisitorsFromOneZoneToAnother() {

        // Create Zone A with 80 visitors and capacity 100.
        Zone zoneA = new Zone();
        zoneA.setId(1);
        zoneA.setName("Zone A");
        zoneA.setCapacity(100);
        zoneA.setInitialVisitors(80);

        // Create Zone B with 30 visitors and capacity 150.
        Zone zoneB = new Zone();
        zoneB.setId(2);
        zoneB.setName("Zone B");
        zoneB.setCapacity(150);
        zoneB.setInitialVisitors(30);

        // Create a connection from Zone A to Zone B.
        ZoneConnection connection = new ZoneConnection();
        connection.setId(1);
        connection.setFromZone(zoneA);
        connection.setToZone(zoneB);
        connection.setMaxVisitorsPerMinute(10);

        // Create our simulation engine.
        CrowdSimulationEngine engine = new CrowdSimulationEngine();

        // Initialize the visitor counts.
        Map<Integer, Integer> currentOccupancy =
                engine.initializeOccupancy(List.of(zoneA, zoneB));

        // Move visitors for one minute.
        Map<Integer, Integer> nextOccupancy =
                engine.moveVisitorsOneMinute(currentOccupancy, connection, 10);

        // Check that the visitor counts are correct.
        assertEquals(70, nextOccupancy.get(1));
        assertEquals(40, nextOccupancy.get(2));

        // Check that the total number of visitors stays the same.
        assertEquals(
                110,
                nextOccupancy.get(1) + nextOccupancy.get(2)
        );
    }
}
