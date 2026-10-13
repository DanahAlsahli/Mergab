package com.example.mergab.Simulation;

import com.example.mergab.Model.Zone;
import com.example.mergab.Model.ZoneConnection;

import java.util.List;
import java.util.HashMap;
import java.util.Map;


/**
 * We'll use this order:
 * 1. Build the simulation logic in Java.
 * 2. Test its calculations with JUnit. (we use System.out.print for a fast tst)
 * 3. Connect it to Spring Boot services and repositories.
 * 4. Expose it through an API and test it with Postman.
 * 5. Connect the frontend.
 **/

public class CrowdSimulationEngine {

    // accept zones for simulations (we can have multiple zones for testing)
    public void testZones(List<Zone> zones) {

        // we can see also how many visitors in each zone
        for (Zone zone : zones) {
            // just to check how many zones it's receive only for testing don't pank
            System.out.println(
                    "Zone: " + zone.getName()
                            + ", Initial visitors: " + zone.getInitialVisitors()
                            + ", Capacity: " + zone.getCapacity()
            );
        }
    }

    // create a map for the zones in the system to track the changes for visitors count in the simulation
    public Map<Integer, Integer> initializeOccupancy(List<Zone> zones) {

        Map<Integer, Integer> occupancy = new HashMap<>();

        for (Zone zone : zones) {
            occupancy.put(zone.getId(), zone.getInitialVisitors());
        }

        return occupancy;
    }

    // print the map we created above for zones and visitors
    public void printOccupancy(Map<Integer, Integer> occupancy) {

        for (Map.Entry<Integer, Integer> entry : occupancy.entrySet()) {
            System.out.println(
                    "Zone ID: " + entry.getKey()
                            + ", Current visitors: " + entry.getValue()
            );
        }
    }

    // Rule 1 in Simulation: move visitors from one zone to another during one minute.
    // We need number of visitors available in Zone A, the connection, and then the remaining capacity in Zone B
    // Important: This method handles only one connection for one minute. We haven’t built the full simulation loop yet.
    public Map<Integer, Integer> moveVisitorsOneMinute(
            Map<Integer, Integer> currentOccupancy,
            ZoneConnection connection,
            int requestedVisitors) {

        Map<Integer, Integer> nextOccupancy =
                new HashMap<>(currentOccupancy);

        Integer fromId = connection.getFromZone().getId();
        Integer toId = connection.getToZone().getId();

        int fromCount = currentOccupancy.getOrDefault(fromId, 0);
        int toCount = currentOccupancy.getOrDefault(toId, 0);

        int targetCapacity = connection.getToZone().getCapacity();

        int allowedVisitors = Math.min(
                Math.max(requestedVisitors, 0),
                connection.getMaxVisitorsPerMinute()
        );

        int movableVisitors = Math.min(allowedVisitors, fromCount);

        int availableSpace = Math.max(0, targetCapacity - toCount);

        movableVisitors = Math.min(movableVisitors, availableSpace);

        nextOccupancy.put(fromId, fromCount - movableVisitors);
        nextOccupancy.put(toId, toCount + movableVisitors);

        return nextOccupancy;
    }

    // This lovely thing text the movement method above if it's work right.
    // helper method
    public void testOneMinuteMovement(
            List<Zone> zones,
            ZoneConnection connection) {

        Map<Integer, Integer> currentOccupancy =
                initializeOccupancy(zones);

        System.out.println("Before movement:");
        printOccupancy(currentOccupancy);

        Map<Integer, Integer> nextOccupancy =
                moveVisitorsOneMinute(currentOccupancy, connection, 10);

        System.out.println("After one minute:");
        printOccupancy(nextOccupancy);
    }


    // Run a simple simulation for a chosen number of minutes.
    // For our first test we will try with 10 visitors request to move every minute.
    public void runSimpleSimulation(
            List<Zone> zones,
            ZoneConnection connection,
            int durationMinutes) {

        Map<Integer, Integer> occupancy =
                initializeOccupancy(zones);

        System.out.println("Minute 0:");
        printOccupancy(occupancy);

        for (int minute = 1; minute <= durationMinutes; minute++) {

            occupancy = moveVisitorsOneMinute(
                    occupancy,
                    connection,
                    10
            );

            System.out.println("Minute " + minute + ":");
            printOccupancy(occupancy);
        }
    }



} // class end
