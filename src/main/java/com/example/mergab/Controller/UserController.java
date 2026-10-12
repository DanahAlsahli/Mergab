package com.example.mergab.Controller;

import com.example.mergab.Api.ApiResponse;
import com.example.mergab.Model.User;
import com.example.mergab.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/get-all")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.status(200).body(userService.getAllUsers());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid User user) {
        userService.register(user);
        return ResponseEntity.status(200).body(new ApiResponse("User registered successfully"));
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal User user, @RequestBody @Valid User newUser) {
        userService.updateUser(user.getId(), newUser);
        return ResponseEntity.status(200).body(new ApiResponse("User updated successfully"));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal User user) {
        userService.deleteUser(user.getId());
        return ResponseEntity.status(200).body(new ApiResponse("User deleted successfully"));
    }
}
