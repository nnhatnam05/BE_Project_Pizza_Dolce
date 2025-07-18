package aptech.be.controllers.staff;

import aptech.be.dto.staff.AdminDecisionDto;
import aptech.be.dto.staff.StaffRequestCreateDto;
import aptech.be.dto.staff.StaffRequestAdminViewDto;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.StaffProfile;
import aptech.be.models.staff.StaffRequest;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.staff.StaffProfileRepository;
import aptech.be.services.staff.StaffRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("api/staff")
public class StaffRequestController {

    private final StaffRequestService staffRequestService;
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;

    @Autowired
    public StaffRequestController(StaffRequestService staffRequestService, UserRepository userRepository, StaffProfileRepository staffProfileRepository) {
        this.staffRequestService = staffRequestService;
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    @PostMapping("/request")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<?> createRequest(@RequestBody StaffRequestCreateDto dto, Principal principal) {
        String username = principal.getName();
        System.out.println("Principal name: " + username);

        // Sửa thành tìm theo username (vì principal.getName() trả username)
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Long userId = user.getId();
        StaffRequest req = staffRequestService.createRequest(userId, dto);
        return ResponseEntity.ok(req);
    }



    @GetMapping("/request")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffRequestAdminViewDto>> getRequestsForAdmin(
            @RequestParam(required = false) String status) {
        List<StaffRequestAdminViewDto> list;
        if (status == null || status.isEmpty()) {
            list = staffRequestService.getAllRequestsForAdmin();
        } else {
            list = staffRequestService.getRequestsByStatusView(status.toUpperCase());
        }
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/request/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveRequest(@PathVariable Long id, @RequestBody AdminDecisionDto decision) {
        StaffRequest req = staffRequestService.approveRequest(id, decision.getAdminNote());
        return ResponseEntity.ok(req);
    }

    @PutMapping("/{id}/request/deny")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> denyRequest(@PathVariable Long id, @RequestBody AdminDecisionDto decision) {
        StaffRequest req = staffRequestService.denyRequest(id, decision.getAdminNote());
        return ResponseEntity.ok(req);
    }


    @GetMapping("/me")
    @PreAuthorize("hasAuthority('ROLE_STAFF')")
    public ResponseEntity<List<StaffRequestAdminViewDto>> getMyRequests(Principal principal) {
        String username = principal.getName();
        UserEntity user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StaffProfile staff = staffProfileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        Long staffId = staff.getId();
        List<StaffRequest> list = staffRequestService.getRequestsByStaff(staffId);
        List<StaffRequestAdminViewDto> dtos = list.stream().map(req -> {
            StaffRequestAdminViewDto dto = new StaffRequestAdminViewDto();
            dto.setId(req.getId());
            dto.setType(req.getType().name());
            dto.setReason(req.getReason());
            dto.setStatus(req.getStatus().name());
            dto.setRequestDate(req.getRequestDate());
            dto.setTargetDate(req.getTargetDate());
            dto.setAdminNote(req.getAdminNote());
            dto.setStaffCode(staff.getStaffCode());
            dto.setStaffName(user.getName());
            dto.setDepartment(staff.getWorkLocation());
            dto.setPosition(staff.getPosition());
            dto.setAvatarUrl(user.getImageUrl());
            return dto;
        }).toList();
        return ResponseEntity.ok(dtos);
    }


}


