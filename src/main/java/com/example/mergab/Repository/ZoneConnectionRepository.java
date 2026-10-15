package com.example.mergab.Repository;

import com.example.mergab.Model.ZoneConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ZoneConnectionRepository extends JpaRepository<ZoneConnection,Integer> {

    ZoneConnection findZoneConnectionById(Integer id);
    boolean existsByFromZone_IdAndToZone_Id(Integer fromZoneId, Integer toZoneId);
    boolean existsByFromZone_IdOrToZone_Id(Integer fromZoneId, Integer toZoneId);
    List<ZoneConnection> findByFromZone_Id(Integer zoneId);
    List<ZoneConnection> findByFromZone_IdOrToZone_Id(Integer fromZoneId, Integer toZoneId);
}
