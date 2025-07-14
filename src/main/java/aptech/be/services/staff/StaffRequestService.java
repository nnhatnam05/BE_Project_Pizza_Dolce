package aptech.be.services.staff;

import aptech.be.dto.staff.StaffRequestAdminViewDto;
import aptech.be.dto.staff.StaffRequestCreateDto;
import aptech.be.models.staff.StaffRequest;

import java.util.List;

public interface StaffRequestService {
    StaffRequest createRequest(Long userId, StaffRequestCreateDto dto);
    List<StaffRequestAdminViewDto> getAllRequestsForAdmin();
    StaffRequest approveRequest(Long requestId, String adminNote);
    StaffRequest denyRequest(Long requestId, String adminNote);
    List<StaffRequest> getRequestsByStaff(Long staffId);
    List<StaffRequestAdminViewDto> getRequestsByStatusView(String status);
}
