package com.example.mergab.Repository;

import com.example.mergab.Model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Integer> {

    Visitor findVisitorById(Integer id);

    Visitor findVisitorByPhoneNumber(String phoneNumber);
}
