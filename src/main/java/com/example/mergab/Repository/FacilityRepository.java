package com.example.mergab.Repository;


import com.example.mergab.Model.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityRepository extends JpaRepository<Facility, Integer> {

    Facility findFacilityById(Integer id);

    List<Facility> findFacilitiesByZone_Id(Integer zoneId);

    List<Facility> findFacilitiesByTypeAndGenderIn(String type, List<String> genders);

    boolean existsByZone_Id(Integer zoneId);
}