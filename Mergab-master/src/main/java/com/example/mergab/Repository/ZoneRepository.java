package com.example.mergab.Repository;

import com.example.mergab.Model.Zone;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface ZoneRepository extends JpaRepository<Zone,Integer> {
    Zone findZoneById(Integer id);
    Zone findZoneByName(String name);

    @Query("SELECT z FROM Zone z WHERE z.initialVisitors < z.capacity")
    List<Zone> findAvailableZones();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT z FROM Zone z WHERE z.id = :id")
    Zone findZoneForUpdate(@Param("id") Integer id);
}
