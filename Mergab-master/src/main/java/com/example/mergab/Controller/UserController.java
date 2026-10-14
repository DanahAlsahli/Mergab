package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.User;
import com.example.mergab.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// endpoints الأدمن (بعد السكيورتي: hasAuthority("ADMIN"))
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
}
