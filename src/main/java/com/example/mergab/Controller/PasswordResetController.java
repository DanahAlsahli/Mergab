package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.ForgotPasswordDTO;
import com.example.mergab.DTO.ResetPasswordDTO;
import com.example.mergab.Service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    // الخطوة ١: يرسل كود على الإيميل (نفس الرد دايم سواء الإيميل مسجل أو لا)
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO dto) {
        passwordResetService.sendCode(dto.getEmail());
        return ResponseEntity.status(200).body(new ApiResponse("If this email is registered, a reset code has been sent"));
    }

    // الخطوة ٢: يتحقق من الكود ويغيّر كلمة المرور
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
        passwordResetService.resetPassword(dto);
        return ResponseEntity.status(200).body(new ApiResponse("Password reset successfully"));
    }
}
