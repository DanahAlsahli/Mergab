package com.example.mergab.Repository;

import com.example.mergab.Model.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, Integer> {

    Organizer findOrganizerById(Integer id);

    Organizer findOrganizerByPhoneNumber(String phoneNumber);
}
