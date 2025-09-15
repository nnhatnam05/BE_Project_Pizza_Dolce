package aptech.be.controllers.admin;

import aptech.be.models.ComplaintCase;
import aptech.be.models.UserEntity;
import aptech.be.repositories.ComplaintCaseRepository;
import aptech.be.repositories.ComplaintSettingsRepository;
import aptech.be.models.ComplaintSettings;
import aptech.be.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/complaints")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class ComplaintAdminController {

    @Autowired
    private ComplaintCaseRepository complaintCaseRepository;
    @Autowired private ComplaintSettingsRepository settingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private aptech.be.services.EmailService emailService;

    // Settings persisted to DB
    private ComplaintSettings getSettingsEntity() {
        return settingsRepository.findById(1L).orElseGet(() -> {
            ComplaintSettings s = new ComplaintSettings();
            s.setId(1L);
            return settingsRepository.save(s);
        });
    }

    @GetMapping
    public List<ComplaintCase> list(@RequestParam(value = "status", required = false) String status) {
        System.out.println("Admin complaints list - status filter: " + status);
        List<ComplaintCase> allCases = complaintCaseRepository.findAll();
        System.out.println("Total cases in DB: " + allCases.size());
        if (status != null && !status.isEmpty()) {
            List<ComplaintCase> filtered = complaintCaseRepository.findByStatus(status);
            System.out.println("Cases with status " + status + ": " + filtered.size());
            return filtered;
        }
        return allCases;
    }

    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        ComplaintSettings s = getSettingsEntity();
        Map<String, Object> m = new HashMap<>();
        m.put("assignedSupportStaffId", s.getAssignedSupportStaffId());
        m.put("autoDecisionEnabled", s.getAutoDecisionEnabled());
        return m;
    }

    @PutMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> body) {
        Object staffId = body.get("assignedSupportStaffId");
        Object auto = body.get("autoDecisionEnabled");
        ComplaintSettings s = getSettingsEntity();
        if (staffId != null) {
            try {
                Long sid = Long.valueOf(staffId.toString());
                Optional<UserEntity> user = userRepository.findById(sid);
                if (user.isEmpty()) return ResponseEntity.badRequest().body("Staff not found");
                s.setAssignedSupportStaffId(sid);
            } catch (NumberFormatException nfe) {
                s.setAssignedSupportStaffId(null);
            }
        }
        if (auto != null) s.setAutoDecisionEnabled(Boolean.parseBoolean(auto.toString()));
        settingsRepository.save(s);
        return ResponseEntity.ok(getSettings());
    }

    public static Long getAssignedSupportStaffId() { return null; }
    public static boolean isAutoDecisionEnabled() { return false; }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var caseOpt = complaintCaseRepository.findById(id);
        if (caseOpt.isEmpty()) return ResponseEntity.notFound().build();
        var c = caseOpt.get();
        c.setStatus("APPROVED");
        if (body.get("refundAmount") != null) c.setRefundAmount(Double.valueOf(body.get("refundAmount").toString()));
        if (body.get("refundQrUrl") != null) c.setRefundQrUrl(String.valueOf(body.get("refundQrUrl")));
        complaintCaseRepository.save(c);
        try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), Map.of("type","ADMIN_APPROVED","caseId", c.getId())); } catch (Exception ignored) {}
        // Send email notification to customer if possible
        try { if (emailService != null) emailService.sendComplaintApprovedEmail(c); } catch (Exception ignored) {}
        return ResponseEntity.ok("APPROVED");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        var caseOpt = complaintCaseRepository.findById(id);
        if (caseOpt.isEmpty()) return ResponseEntity.notFound().build();
        var c = caseOpt.get();
        c.setStatus("REJECTED");
        complaintCaseRepository.save(c);
        try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), Map.of("type","ADMIN_REJECTED","caseId", c.getId())); } catch (Exception ignored) {}
        // Email reject reason to customer
        try { if (emailService != null) emailService.sendComplaintRejectedEmail(c, body != null ? String.valueOf(body.getOrDefault("reason", "")) : ""); } catch (Exception ignored) {}
        return ResponseEntity.ok("REJECTED");
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignStaff(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var caseOpt = complaintCaseRepository.findById(id);
        if (caseOpt.isEmpty()) return ResponseEntity.notFound().build();
        Object staffId = body.get("staffId");
        if (staffId == null) return ResponseEntity.badRequest().body("Missing staffId");
        Long sid = Long.valueOf(staffId.toString());
        var staffOpt = userRepository.findById(sid);
        if (staffOpt.isEmpty()) return ResponseEntity.badRequest().body("Staff not found");
        var c = caseOpt.get();
        c.setAssignedStaff(staffOpt.get());
        complaintCaseRepository.save(c);
        try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), Map.of("type","STAFF_ASSIGNED","caseId", c.getId(), "staffId", sid)); } catch (Exception ignored) {}
        return ResponseEntity.ok(Map.of("caseId", c.getId(), "staffId", sid));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        var c = complaintCaseRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        
        String status = (String) body.get("status");
        if (status != null) c.setStatus(status);
        
        String decisionType = (String) body.get("decisionType");
        if (decisionType != null) c.setDecisionType(decisionType);
        
        Object refundAmount = body.get("refundAmount");
        if (refundAmount != null) c.setRefundAmount(Double.valueOf(refundAmount.toString()));
        
        complaintCaseRepository.save(c);
        return ResponseEntity.ok(Map.of("message", "Status updated"));
    }
}



