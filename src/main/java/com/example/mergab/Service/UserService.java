package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.User;
import com.example.mergab.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// عمليات الأدمن على كل المستخدمين
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final VisitorRepository visitorRepository;
    private final ExpoEventRepository expoEventRepository;
    private final SimulationRepository simulationRepository;
    private final ZoneVisitRepository zoneVisitRepository;

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

    @Transactional
    public void deleteUser(Integer id) {
        User user = getUserById(id);

        if ("ORGANIZER".equals(user.getRole())) {

            // Keep events and simulations, but remove their organizer reference
            for (ExpoEvent event : expoEventRepository.findExpoEventsByOrganizer_Id(id)) {
                event.setOrganizer(null);
                expoEventRepository.save(event);
            }

            for (Simulation simulation : simulationRepository.findSimulationsByOrganizer_Id(id)) {
                simulation.setOrganizer(null);
                simulationRepository.save(simulation);
            }

            // Delete organizer profile before deleting the user account
            user.setOrganizer(null);
            organizerRepository.deleteById(id);
            organizerRepository.flush();
        }

        if ("VISITOR".equals(user.getRole())) {

            // Prevent deleting a visitor with zone visit history
            if (zoneVisitRepository.existsByVisitor_Id(id)) {
                throw new ApiException("Cannot delete a visitor with zone visit history");
            }

            // Delete visitor profile before deleting the user account
            user.setVisitor(null);
            visitorRepository.deleteById(id);
            visitorRepository.flush();
        }

        // Delete the user account (ADMIN, ORGANIZER, or VISITOR)
        userRepository.delete(user);
    }
}
