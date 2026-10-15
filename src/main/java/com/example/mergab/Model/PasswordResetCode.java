package com.example.mergab.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// كود استعادة كلمة المرور (٦ أرقام) يوصل للمستخدم على الإيميل
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // لو انحذف المستخدم تنحذف أكواده معه
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // نخزن الكود مشفّر زي الباسوورد، مو نص عادي
    @Column(nullable = false)
    private String codeHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // عدد المحاولات الغلط (الحد ٥)
    @Column(nullable = false)
    private Integer attempts = 0;
}
