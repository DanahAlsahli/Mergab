package com.example.mergab.Repository;

import com.example.mergab.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserById(Integer id);

    User findUserByEmail(String email);

    List<User> findUsersByRole(String role);
}
