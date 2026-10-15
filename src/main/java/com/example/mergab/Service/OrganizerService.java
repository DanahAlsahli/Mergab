package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.DTO.OrganizerDTO;
import com.example.mergab.Model.ExpoEvent;
import com.example.mergab.Model.Organizer;
import com.example.mergab.Model.Simulation;
import com.example.mergab.Model.User;
import com.example.mergab.Repository.ExpoEventRepository;
import com.example.mergab.Repository.OrganizerRepository;
import com.example.mergab.Repository.SimulationRepository;
import com.example.mergab.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final ExpoEventRepository expoEventRepository;
    private final SimulationRepository simulationRepository;
    private final PasswordEncoder passwordEncoder;

    public List<User> getAllOrganizers() {
        return userRepository.findUsersByRole("ORGANIZER");
    }

    @Transactional
    public void register(OrganizerDTO dto) {
        if (userRepository.findUserByEmail(dto.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }
        if (organizerRepository.findOrganizerByPhoneNumber(dto.getPhoneNumber()) != null) {
            throw new ApiException("Phone number already exists");
        }

        User user = new User(null, dto.getFullName(), dto.getEmail(), passwordEncoder.encode(dto.getPassword()), "ORGANIZER", null, null);
        userRepository.save(user);

        Organizer organizer = new Organizer(null, dto.getPhoneNumber(), user, null, null);
        user.setOrganizer(organizer);
        organizerRepository.save(organizer);
    }



    @Transactional
    public void updateMyPhone(Integer userId, String phoneNumber) {
        Organizer organizer = organizerRepository.findOrganizerById(userId);

        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }

        if (phoneNumber == null || !phoneNumber.matches("^05\\d{8}$")) {
            throw new ApiException("Invalid phone number");
        }

        Organizer existing = organizerRepository.findOrganizerByPhoneNumber(phoneNumber);

        if (existing != null && !existing.getId().equals(organizer.getId())) {
            throw new ApiException("Phone number already exists");
        }

        organizer.setPhoneNumber(phoneNumber);
        organizerRepository.save(organizer);
    }

    @Transactional
    public void deleteOrganizer(Integer organizerId) {
        Organizer organizer = organizerRepository.findOrganizerById(organizerId);
        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }

        // نفك ربط شغله قبل الحذف عشان تبقى الفعاليات والمحاكاة
        for (ExpoEvent event : expoEventRepository.findExpoEventsByOrganizer_Id(organizerId)) {
            event.setOrganizer(null);
            expoEventRepository.save(event);
        }
        for (Simulation simulation : simulationRepository.findSimulationsByOrganizer_Id(organizerId)) {
            simulation.setOrganizer(null);
            simulationRepository.save(simulation);
        }
        User user = organizer.getUser();

        user.setOrganizer(null);
        organizerRepository.delete(organizer);
        organizerRepository.flush();

        userRepository.delete(user);

    }
}
