package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.User;
import com.example.mergab.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// عمليات الأدمن على كل المستخدمين
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Integer id) {
        User user = userRepository.findUserById(id);
        if (user == null) {
            throw new ApiException("User not found");
        }
        return user;
    }

    public List<User> getUsersByRole(String role) {
        role = role.toUpperCase();
        if (!role.matches("^(ADMIN|ORGANIZER|VISITOR)$")) {
            throw new ApiException("Role must be ADMIN, ORGANIZER or VISITOR");
        }
        return userRepository.findUsersByRole(role);
    }

    // الأدمن ما له تسجيل عام، يضيفه أدمن ثاني (أو يُدخل يدويًا أول مرة)
    public void addAdmin(User admin) {
        if (userRepository.findUserByEmail(admin.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }
        // TODO (Security): admin.setPassword(new BCryptPasswordEncoder().encode(admin.getPassword()));
        admin.setRole("ADMIN");
        admin.setOrganizer(null);
        admin.setVisitor(null);
        userRepository.save(admin);
    }

    public void deleteUser(Integer id) {
        User user = getUserById(id);
        userRepository.delete(user); // cascade يحذف Organizer/Visitor المرتبط
    }
}
