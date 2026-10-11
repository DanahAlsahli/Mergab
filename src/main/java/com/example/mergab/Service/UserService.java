package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.User;
import com.example.mergab.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void register(User user) {
        if (userRepository.findUserByEmail(user.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));
        user.setRole("ORGANIZER");
        userRepository.save(user);
    }

    public void updateUser(Integer userId, User newUser) {
        User oldUser = userRepository.findUserById(userId);
        if (oldUser == null) {
            throw new ApiException("User not found");
        }

        User emailOwner = userRepository.findUserByEmail(newUser.getEmail());
        if (emailOwner != null && !emailOwner.getId().equals(userId)) {
            throw new ApiException("Email already exists");
        }

        oldUser.setFullName(newUser.getFullName());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setPassword(new BCryptPasswordEncoder().encode(newUser.getPassword()));

        userRepository.save(oldUser);
    }

    public void deleteUser(Integer userId) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new ApiException("User not found");
        }
        userRepository.delete(user);
    }
}
