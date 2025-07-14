package aptech.be.services.staff;

import aptech.be.dto.staff.StaffRequestAdminViewDto;
import aptech.be.dto.staff.StaffRequestCreateDto;
import aptech.be.models.Notification;
import aptech.be.models.UserEntity;
import aptech.be.models.staff.RequestStatus;
import aptech.be.models.staff.RequestType;
import aptech.be.models.staff.StaffProfile;
import aptech.be.models.staff.StaffRequest;
import aptech.be.repositories.NotificationRepository;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.staff.StaffProfileRepository;
import aptech.be.repositories.staff.StaffRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StaffRequestServiceImpl implements StaffRequestService {
    private final StaffRequestRepository staffRequestRepo;
    private final StaffProfileRepository staffProfileRepo;
    private final UserRepository userRepo;
    @Autowired
    private NotificationRepository notificationRepo;
    // Để lấy thông tin admin


    public StaffRequestServiceImpl(StaffRequestRepository staffRequestRepo,
                                   StaffProfileRepository staffProfileRepo,
                                   UserRepository userRepo) {
        this.staffRequestRepo = staffRequestRepo;
        this.staffProfileRepo = staffProfileRepo;
        this.userRepo = userRepo;
    }


    @Override
    public StaffRequest createRequest(Long userId, StaffRequestCreateDto dto) {
        var user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var staff = staffProfileRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));

        StaffRequest req = new StaffRequest();
        req.setStaff(staff);
        req.setType(RequestType.valueOf(dto.getType()));
        req.setReason(dto.getReason());
        req.setStatus(RequestStatus.PENDING);
        req.setRequestDate(java.time.LocalDate.now());
        req.setTargetDate(dto.getTargetDate());
        req.setAdminNote(null);

        StaffRequest request = staffRequestRepo.save(req);

        // TẠO NOTIFICATION CHO ADMIN
        // Giả sử tất cả user role "ADMIN" đều nhận thông báo này
        List<UserEntity> admins = userRepo.findByRole("ADMIN");
        for (UserEntity admin : admins) {
            Notification noti = new Notification();
            noti.setTargetUserId(admin.getId());
            noti.setMessage("You have a new request from an employee " + user.getName());
            noti.setType("REQUEST_NEW");
            noti.setData("{\"requestId\":" + request.getId() + "}");
            noti.setIsRead(false);
            noti.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepo.save(noti);
        }

        return request;
    }



    @Override
    public List<StaffRequestAdminViewDto> getAllRequestsForAdmin() {
        var allRequests = staffRequestRepo.findAll();
        return allRequests.stream().map(req -> {
            var staff = req.getStaff();
            var user = staff != null ? staff.getUser() : null;
            StaffRequestAdminViewDto dto = new StaffRequestAdminViewDto();
            dto.setId(req.getId());
            dto.setType(req.getType().name());
            dto.setReason(req.getReason());
            dto.setStatus(req.getStatus().name());
            dto.setRequestDate(req.getRequestDate());
            dto.setTargetDate(req.getTargetDate());
            dto.setAdminNote(req.getAdminNote());

            // Thông tin nhân viên
            if (staff != null) {
                dto.setStaffCode(staff.getStaffCode());
                dto.setDepartment(staff.getWorkLocation());
                dto.setPosition(staff.getPosition());
                if (user != null) {
                    dto.setStaffName(user.getName());
                    dto.setAvatarUrl(user.getImageUrl());
                }
            }
            return dto;
        }).toList();
    }


    @Override
    public StaffRequest approveRequest(Long requestId, String adminNote) {
        var req = staffRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (req.getStatus() != RequestStatus.PENDING)
            throw new RuntimeException("Request is already processed!");
        req.setStatus(RequestStatus.APPROVED);
        req.setAdminNote(adminNote);
        StaffRequest updated = staffRequestRepo.save(req);

        // TẠO NOTIFICATION CHO STAFF
        Notification noti = new Notification();
        noti.setTargetUserId(req.getStaff().getUser().getId());
        noti.setMessage("Your request has been APPROVED by admin.");
        noti.setType("REQUEST_CONFIRMED");
        noti.setData("{\"requestId\":" + req.getId() + "}");
        noti.setIsRead(false);
        noti.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepo.save(noti);

        return updated;
    }

    @Override
    public StaffRequest denyRequest(Long requestId, String adminNote) {
        var req = staffRequestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        if (req.getStatus() != RequestStatus.PENDING)
            throw new RuntimeException("Request is already processed!");
        req.setStatus(RequestStatus.DENIED);
        req.setAdminNote(adminNote);
        StaffRequest updated = staffRequestRepo.save(req);

        // TẠO NOTIFICATION CHO STAFF
        Notification noti = new Notification();
        noti.setTargetUserId(req.getStaff().getUser().getId());
        noti.setMessage("Your request has been DENIED by admin.");
        noti.setType("REQUEST_CONFIRMED");
        noti.setData("{\"requestId\":" + req.getId() + "}");
        noti.setIsRead(false);
        noti.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepo.save(noti);

        return updated;
    }



    @Override
    public List<StaffRequest> getRequestsByStaff(Long staffId) {
        var staff = staffProfileRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff profile not found"));
        return staffRequestRepo.findByStaff(staff);
    }

    public List<StaffRequestAdminViewDto> getRequestsByStatusView(String status) {
        RequestStatus statusEnum = RequestStatus.valueOf(status.toUpperCase());
        List<StaffRequest> list = staffRequestRepo.findByStatus(statusEnum);
        return list.stream()
                .map(this::toAdminDto)
                .collect(Collectors.toList());
    }

    private StaffRequestAdminViewDto toAdminDto(StaffRequest req) {
        StaffProfile staff = req.getStaff();
        UserEntity user = staff.getUser();
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
    }






}
