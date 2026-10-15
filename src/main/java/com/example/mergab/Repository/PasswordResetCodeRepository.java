package com.example.mergab.Repository;

import com.example.mergab.Model.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Integer> {

    PasswordResetCode findFirstByUser_IdOrderByIdDesc(Integer userId);

    void deleteByUser_Id(Integer userId);
}
