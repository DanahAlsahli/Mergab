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
}
