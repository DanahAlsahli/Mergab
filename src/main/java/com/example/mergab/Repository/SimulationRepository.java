package com.example.mergab.Repository;

import com.example.mergab.Model.Simulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Integer> {
    Simulation findSimulationById(Integer id);
    List<Simulation> findSimulationsByOrganizer_Id(Integer organizerId);

}
