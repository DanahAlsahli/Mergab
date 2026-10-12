package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.DTO.OrganizerDTO;
import com.example.mergab.Model.Organizer;
import com.example.mergab.Model.User;
import com.example.mergab.Repository.OrganizerRepository;
import com.example.mergab.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;

    public List<User> getAllOrganizers() {
        return userRepository.findUsersByRole("ORGANIZER");
    }

    public void register(OrganizerDTO dto) {
        if (userRepository.findUserByEmail(dto.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }
        if (organizerRepository.findOrganizerByPhoneNumber(dto.getPhoneNumber()) != null) {
            throw new ApiException("Phone number already exists");
        }

        User user = new User(null, dto.getFullName(), dto.getEmail(), dto.getPassword(), "ORGANIZER", null, null);
        userRepository.save(user);

        Organizer organizer = new Organizer(null, dto.getOrganizationName(), dto.getPhoneNumber(), user, null, null);
        organizerRepository.save(organizer);
    }

    public void updateOrganizer(Integer organizerId, OrganizerDTO dto) {
        Organizer organizer = organizerRepository.findOrganizerById(organizerId);
        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }

        User emailOwner = userRepository.findUserByEmail(dto.getEmail());
        if (emailOwner != null && !emailOwner.getId().equals(organizerId)) {
            throw new ApiException("Email already exists");
        }
        Organizer phoneOwner = organizerRepository.findOrganizerByPhoneNumber(dto.getPhoneNumber());
        if (phoneOwner != null && !phoneOwner.getId().equals(organizerId)) {
            throw new ApiException("Phone number already exists");
        }

        User user = organizer.getUser();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        organizer.setOrganizationName(dto.getOrganizationName());
        organizer.setPhoneNumber(dto.getPhoneNumber());

        userRepository.save(user);
        organizerRepository.save(organizer);
    }

    public void deleteOrganizer(Integer organizerId) {
        Organizer organizer = organizerRepository.findOrganizerById(organizerId);
        if (organizer == null) {
            throw new ApiException("Organizer not found");
        }
        userRepository.delete(organizer.getUser());
    }
}
