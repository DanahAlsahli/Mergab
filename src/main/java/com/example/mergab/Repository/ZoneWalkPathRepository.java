package com.example.mergab.Repository;



import com.example.mergab.Model.ZoneWalkPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ZoneWalkPathRepository extends JpaRepository<ZoneWalkPath, Integer> {

    ZoneWalkPath findZoneWalkPathById(Integer id);

    List<ZoneWalkPath> findAllByZone_Id(Integer zoneId);

    List<ZoneWalkPath> findAllByZone_IdAndActiveTrue(Integer zoneId);
}
