package com.example.mergab.Repository;

import com.example.mergab.Model.Visitor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Integer> {

    Visitor findVisitorById(Integer id);

    Visitor findVisitorByPhoneNumber(String phoneNumber);
    Visitor findVisitorByQrToken(String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Visitor v WHERE v.qrToken = :qrToken")
    Visitor findVisitorForUpdate(@Param("qrToken") String qrToken);
}
