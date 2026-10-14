package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.DTO.ChangePasswordDTO;
import com.example.mergab.Model.User;
import com.example.mergab.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.status(200).body(userService.getAllUsers());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        return ResponseEntity.status(200).body(userService.getUserById(id));
    }

    @GetMapping("/get-by-role/{role}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.status(200).body(userService.getUsersByRole(role));
    }

    @PostMapping("/add-admin")
    public ResponseEntity<?> addAdmin(@RequestBody @Valid User admin) {
        userService.addAdmin(admin);
        return ResponseEntity.status(201).body(new ApiResponse("Admin added successfully"));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.status(200).body(new ApiResponse("User deleted successfully"));
    }

    @GetMapping("/my-account")
    public ResponseEntity<?> getMyAccount(@AuthenticationPrincipal User user) {
        return ResponseEntity.status(200).body(userService.getUserById(user.getId()));
    }

    @PutMapping("/update-my-account")
    public ResponseEntity<?> updateMyAccount(@AuthenticationPrincipal User user, @RequestBody User updatedUser) {

        userService.updateMyAccount(user.getId(), updatedUser.getFullName(), updatedUser.getEmail());

        return ResponseEntity.status(200).body(new ApiResponse("Account updated successfully"));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user, @RequestBody @Valid ChangePasswordDTO request) {

        userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());

        return ResponseEntity.status(200).body(new ApiResponse("Password changed successfully"));
    }

    @DeleteMapping("/delete-my-account")
    public ResponseEntity<?> deleteMyAccount(@AuthenticationPrincipal User user) {
        userService.deleteUser(user.getId());

        return ResponseEntity.status(200).body(new ApiResponse("Account deleted successfully"));
    }
}
