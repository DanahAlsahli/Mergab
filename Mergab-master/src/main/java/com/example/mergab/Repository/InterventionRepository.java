package com.example.mergab.Repository;

import com.example.mergab.Model.Intervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterventionRepository extends JpaRepository<Intervention, Integer> {

    Intervention findInterventionById(Integer id);
    List<Intervention> findBySimulation_Id(Integer simulationId);

}
