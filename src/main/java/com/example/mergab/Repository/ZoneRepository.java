package com.example.mergab.Repository;

import com.example.mergab.Model.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ZoneRepository extends JpaRepository<Zone,Integer> {
    Zone findZoneById(Integer id);
    Zone findZoneByName(String name);

    @Query("SELECT z FROM Zone z WHERE z.initialVisitors < z.capacity")
    List<Zone> findAvailableZones();
}
