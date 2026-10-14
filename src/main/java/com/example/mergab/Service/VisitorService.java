package com.example.mergab.Service;

import com.example.mergab.Api.ApiException;
import com.example.mergab.DTO.VisitorDTO;
import com.example.mergab.Model.User;
import com.example.mergab.Model.Visitor;
import com.example.mergab.Repository.UserRepository;
import com.example.mergab.Repository.VisitorRepository;
import com.example.mergab.Repository.ZoneVisitRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;
    private final ZoneVisitRepository zoneVisitRepository;

    public List<User> getAllVisitors() {
        return userRepository.findUsersByRole("VISITOR");
    }

    @Transactional
    public void register(VisitorDTO dto) {
        if (userRepository.findUserByEmail(dto.getEmail()) != null) {
            throw new ApiException("Email already exists");
        }
        if (visitorRepository.findVisitorByPhoneNumber(dto.getPhoneNumber()) != null) {
            throw new ApiException("Phone number already exists");
        }

        User user = new User(null, dto.getFullName(), dto.getEmail(), dto.getPassword(), "VISITOR", null, null);
        userRepository.save(user);

        Visitor visitor = new Visitor(null, dto.getPhoneNumber(), UUID.randomUUID().toString(), user, null,
                Boolean.TRUE.equals(dto.getShowPrayerTimes()));
        user.setVisitor(visitor);
        visitorRepository.save(visitor);


    }

    @Transactional
    public void updateVisitor(Integer visitorId, VisitorDTO dto) {
        Visitor visitor = visitorRepository.findVisitorById(visitorId);
        if (visitor == null) {
            throw new ApiException("Visitor not found");
        }

        User emailOwner = userRepository.findUserByEmail(dto.getEmail());
        if (emailOwner != null && !emailOwner.getId().equals(visitorId)) {
            throw new ApiException("Email already exists");
        }
        Visitor phoneOwner = visitorRepository.findVisitorByPhoneNumber(dto.getPhoneNumber());
        if (phoneOwner != null && !phoneOwner.getId().equals(visitorId)) {
            throw new ApiException("Phone number already exists");
        }

        User user = visitor.getUser();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        visitor.setPhoneNumber(dto.getPhoneNumber());
        visitor.setShowPrayerTimes(Boolean.TRUE.equals(dto.getShowPrayerTimes()));

        userRepository.save(user);
        visitorRepository.save(visitor);
    }

    @Transactional
    public void deleteVisitor(Integer visitorId) {
        Visitor visitor = visitorRepository.findVisitorById(visitorId);

        if (visitor == null) {
            throw new ApiException("Visitor not found");
        }
        if (zoneVisitRepository.existsByVisitor_Id(visitorId)) {
            throw new ApiException("Cannot delete a visitor with zone visit history");
        }

        User user = visitor.getUser();

        user.setVisitor(null);

        visitorRepository.delete(visitor);
        visitorRepository.flush();

        userRepository.delete(user);
    }


    // Generate the visitor's QR code image
    public byte[] getVisitorQrCode(Integer visitorId) {

        Visitor visitor = visitorRepository.findVisitorById(visitorId);

        if (visitor == null) {
            throw new ApiException("Visitor not found");
        }

        return qrCodeService.generateQrCode(visitor.getQrToken());
    }

}
