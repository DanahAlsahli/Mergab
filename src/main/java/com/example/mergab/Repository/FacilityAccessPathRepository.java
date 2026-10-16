package com.example.mergab.Repository;



import com.example.mergab.Model.FacilityAccessPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FacilityAccessPathRepository extends JpaRepository<FacilityAccessPath, Integer> {

    FacilityAccessPath findFacilityAccessPathById(Integer id);

    List<FacilityAccessPath> findAllByFacility_Id(Integer facilityId);

    List<FacilityAccessPath> findAllByFacility_IdAndActiveTrue(Integer facilityId);

    List<FacilityAccessPath> findAllByZoneWalkPath_Id(Integer zoneWalkPathId);

    boolean existsByZoneWalkPath_Id(Integer zoneWalkPathId);

    boolean existsByFacility_Id(Integer facilityId);
}
