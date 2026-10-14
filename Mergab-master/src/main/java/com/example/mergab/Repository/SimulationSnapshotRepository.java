package com.example.mergab.Repository;

import com.example.mergab.Model.SimulationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface SimulationSnapshotRepository extends JpaRepository<SimulationSnapshot,Integer> {

    SimulationSnapshot findSimulationSnapshotById(Integer id);

    List<SimulationSnapshot> findByZone_Id(Integer zoneId);

    List<SimulationSnapshot> findByZone_IdOrderByMinuteAsc(Integer zoneId);

    List<SimulationSnapshot> findBySimulation_Id(Integer simulationId);

    List<SimulationSnapshot> findBySimulation_IdAndZone_IdOrderByMinuteAsc(Integer simulationId, Integer zoneId);
    boolean existsBySimulation_IdAndZone_IdAndMinute(Integer simulationId, Integer zoneId, Integer minute);
    List<SimulationSnapshot> findBySimulation_IdAndMinute(Integer simulationId, Integer minute);
    SimulationSnapshot findFirstBySimulation_IdAndZone_IdOrderByOccupancyDesc(Integer simulationId, Integer zoneId);



}
