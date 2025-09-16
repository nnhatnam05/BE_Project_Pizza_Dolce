package aptech.be.controllers.staff;

import aptech.be.models.ComplaintCase;
import aptech.be.models.ComplaintMessage;
import aptech.be.models.UserEntity;
import aptech.be.repositories.ComplaintCaseRepository;
import aptech.be.repositories.ComplaintMessageRepository;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.ComplaintSettingsRepository;
import aptech.be.models.ComplaintSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@RestController
@RequestMapping("/api/staff/complaints")
@PreAuthorize("hasAnyRole('STAFF','ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class ComplaintStaffController {
    @Autowired private ComplaintCaseRepository complaintRepo;
    @Autowired private ComplaintMessageRepository messageRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ComplaintSettingsRepository settingsRepo;
    @Autowired private aptech.be.repositories.OrderRepository orderRepo;
    @Autowired private aptech.be.repositories.FoodRepository foodRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired(required = false) private aptech.be.services.EmailService emailService;

    private ComplaintSettings getSettings() {
        return settingsRepo.findById(1L).orElseGet(() -> {
            ComplaintSettings s = new ComplaintSettings();
            s.setId(1L);
            return settingsRepo.save(s);
        });
    }

    private boolean isAllowed(UserEntity me) {
        if (me == null) return false;
        if ("ADMIN".equalsIgnoreCase(me.getRole())) return true;
        Long assignedId = getSettings().getAssignedSupportStaffId();
        return assignedId != null && me.getId() != null && me.getId().equals(assignedId);
    }

    @GetMapping("/permission")
    public ResponseEntity<?> permission(Authentication auth) {
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        boolean allowed = isAllowed(me);
        Long currentUserId = me != null ? me.getId() : null;
        String currentUserRole = me != null ? me.getRole() : null;
        Long assignedId = getSettings().getAssignedSupportStaffId();
        return ResponseEntity.ok(java.util.Map.of(
                "allowed", allowed,
                "currentUserId", currentUserId,
                "currentUserRole", currentUserRole,
                "assignedSupportStaffId", assignedId
        ));
    }

    @GetMapping
    public ResponseEntity<?> myAssigned(Authentication auth) {
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        // Return cases assigned to me, plus unassigned (to avoid losing pending items before admin assigns)
        List<ComplaintCase> mine = complaintRepo.findByAssignedStaff(me);
        try {
            List<ComplaintCase> unassigned = complaintRepo.findByAssignedStaffIsNull();
            mine.addAll(unassigned);
        } catch (Exception ignored) {}
        return ResponseEntity.ok(mine);
    }

    // Resolved list for staff: mirror admin's resolved view
    @GetMapping("/resolved")
    public ResponseEntity<?> resolved(Authentication auth) {
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        var statuses = java.util.Arrays.asList("RESOLVED","APPROVED","REJECTED");
        var cases = complaintRepo.findByStatusIn(statuses);
        return ResponseEntity.ok(cases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        // Auto-assign to current staff if complaint is unassigned to avoid NPE and allow handling
        if (c.getAssignedStaff() == null) {
            c.setAssignedStaff(me);
            complaintRepo.save(c);
        } else if (!me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(c);
    }

    @PostMapping("/{id}/qr")
    public ResponseEntity<?> uploadRefundQr(@PathVariable Long id, @RequestParam("file") MultipartFile file, Authentication auth) {
        try {
            var c = complaintRepo.findById(id).orElse(null);
            if (c == null) return ResponseEntity.notFound().build();
            String email = auth.getName();
            UserEntity me = userRepo.findByEmail(email).orElse(null);
            if (!isAllowed(me)) return ResponseEntity.status(403).build();
            if (me == null) return ResponseEntity.status(401).build();
            if (c.getAssignedStaff() != null && !me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole()))
                return ResponseEntity.status(403).build();

            if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("Missing file");
            String original = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "qr.png";
            String filename = Instant.now().toEpochMilli() + "_" + original;
            Path uploadDir = Paths.get("uploads");
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
            Path target = uploadDir.resolve(filename);
            Files.write(target, file.getBytes());
            String url = "/uploads/" + filename;
            // store on complaint for convenience
            c.setRefundQrUrl(url);
            complaintRepo.save(c);
            return ResponseEntity.ok(java.util.Map.of("url", url));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Upload failed");
        }
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<?> messages(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(messageRepo.findByComplaintIdOrderByCreatedAtAsc(id));
    }

    // Unified timeline for staff (messages + attachments)
    @GetMapping("/{id}/timeline")
    public ResponseEntity<?> timeline(@PathVariable Long id, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        var items = new java.util.ArrayList<java.util.Map<String,Object>>();
        for (var m : c.getMessages()) {
            var map = new java.util.HashMap<String,Object>();
            map.put("type", "message");
            map.put("id", m.getId());
            map.put("senderType", m.getSenderType());
            map.put("senderId", m.getSenderId());
            map.put("message", m.getMessage());
            map.put("createdAt", m.getCreatedAt());
            items.add(map);
        }
        for (var a : c.getAttachments()) {
            var map = new java.util.HashMap<String,Object>();
            map.put("type", "attachment");
            map.put("id", a.getId());
            map.put("url", a.getUrl());
            map.put("mimeType", a.getMimeType());
            map.put("uploadedBy", a.getUploadedBy());
            map.put("createdAt", a.getCreatedAt());
            items.add(map);
        }
        items.sort((x,y) -> {
            java.time.LocalDateTime dx = (java.time.LocalDateTime) x.get("createdAt");
            java.time.LocalDateTime dy = (java.time.LocalDateTime) y.get("createdAt");
            return dx.compareTo(dy);
        });
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<?> postMessage(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        // Auto-assign complaint to current staff if unassigned
        if (c.getAssignedStaff() == null) {
            c.setAssignedStaff(me);
            complaintRepo.save(c);
        } else if (!me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole())) {
            return ResponseEntity.status(403).build();
        }
        String msg = String.valueOf(body.getOrDefault("message", "")).trim();
        if (msg.isEmpty()) return ResponseEntity.badRequest().body("Message cannot be empty");
        ComplaintMessage m = new ComplaintMessage();
        m.setComplaint(c);
        m.setSenderType("STAFF");
        m.setSenderId(me.getId());
        m.setMessage(msg);
        messageRepo.save(m);
        messagingTemplate.convertAndSend("/topic/complaints/" + id, m);
        return ResponseEntity.ok(m.getId());
    }

    // Auto ON: staff finalize decision (refund or re-delivery)
    @PostMapping("/{id}/decide")
    public ResponseEntity<?> decide(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        if (!me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole()))
            return ResponseEntity.status(403).build();

        String action = String.valueOf(body.getOrDefault("action", "")); // REFUND or REDELIVER or REJECT
        Double refundAmount = body.get("refundAmount") != null ? Double.valueOf(body.get("refundAmount").toString()) : null;
        String refundQrUrl = body.get("refundQrUrl") != null ? String.valueOf(body.get("refundQrUrl")) : null;
        Double redeliveryTotal = body.get("redeliveryTotal") != null ? Double.valueOf(body.get("redeliveryTotal").toString()) : 0.0;
        String rejectReason = body.get("reason") != null ? String.valueOf(body.get("reason")) : null;

        if (Boolean.TRUE.equals(c.getAutoDecisionEnabledSnapshot())) {
            if ("REFUND".equalsIgnoreCase(action)) {
                c.setDecisionType("REFUND");
                c.setRefundAmount(refundAmount != null ? refundAmount : c.getOrder().getTotalPrice());
                c.setRefundQrUrl(refundQrUrl);
                c.setRefundStatus("PENDING");
                c.setDecidedByStaffId(me.getId());
                c.setStatus("RESOLVED");
                try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","STAFF_DECIDE_REFUND","caseId", c.getId())); } catch (Exception ignored) {}
            } else if ("REJECT".equalsIgnoreCase(action)) {
                c.setDecisionType("NONE");
                c.setStaffNote(rejectReason);
                c.setDecidedByStaffId(me.getId());
                c.setStatus("REJECTED");
                complaintRepo.save(c);
                try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","STAFF_REJECTED","caseId", c.getId())); } catch (Exception ignored) {}
                try { if (emailService != null) emailService.sendComplaintRejectedEmail(c, rejectReason != null ? rejectReason : ""); } catch (Exception ignored) {}
                return ResponseEntity.ok("REJECTED");
            } else if ("REDELIVER".equalsIgnoreCase(action)) {
                c.setDecisionType("REDELIVER");
                c.setRefundAmount(0.0);
                c.setDecidedByStaffId(me.getId());
                c.setStatus("RESOLVED");
                try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","STAFF_DECIDE_REDELIVER","caseId", c.getId())); } catch (Exception ignored) {}
                // reDeliveryOrderId sẽ được tạo ở bước thực thi (API khác) – ở đây chỉ chốt quyết định
            } else {
                return ResponseEntity.badRequest().body("Invalid action");
            }
            complaintRepo.save(c);
            return ResponseEntity.ok("DECIDED");
        } else {
            if ("REJECT".equalsIgnoreCase(action)) {
                // Auto OFF: yêu cầu admin duyệt từ chối
                c.setStatus("NEED_ADMIN_APPROVAL");
                c.setDecisionType("REJECT");
                c.setStaffNote(rejectReason);
                c.setDecidedByStaffId(me.getId());
                complaintRepo.save(c);
                try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","STAFF_SUBMIT_APPROVAL","caseId", c.getId())); } catch (Exception ignored) {}
                return ResponseEntity.ok("SUBMITTED_FOR_APPROVAL");
            }
            c.setStatus("NEED_ADMIN_APPROVAL");
            c.setDecisionType(action.toUpperCase());
            c.setRefundAmount(refundAmount);
            c.setRefundQrUrl(refundQrUrl);
            c.setDecidedByStaffId(me.getId());
            complaintRepo.save(c);
            try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","STAFF_SUBMIT_APPROVAL","caseId", c.getId())); } catch (Exception ignored) {}
            return ResponseEntity.ok("SUBMITTED_FOR_APPROVAL");
        }
    }

    // Mark refund as completed after staff executes bank transfer
    @PostMapping("/{id}/refund/complete")
    public ResponseEntity<?> completeRefund(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        if (!me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole()))
            return ResponseEntity.status(403).build();
        if (!"REFUND".equalsIgnoreCase(c.getDecisionType())) return ResponseEntity.badRequest().body("Decision is not REFUND");
        c.setRefundStatus("COMPLETED");
        if (body.get("refundReference") != null) c.setRefundReference(String.valueOf(body.get("refundReference")));
        complaintRepo.save(c);
        try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","REFUND_COMPLETED","caseId", c.getId())); } catch (Exception ignored) {}
        try {
            var customer = c.getCustomer();
            if (customer != null && customer.getEmail() != null) {
                // Gửi mail thông báo refund đã hoàn tất (tận dụng EmailService hoặc gửi text đơn giản)
                // Ở đây ta dùng Subject đơn giản, nội dung có thể nâng cấp template sau
                // Optional: inject EmailService để gửi HTML bài bản
            }
        } catch (Exception ignored) {}
        return ResponseEntity.ok("REFUND_COMPLETED");
    }

    // Create replacement order for re-delivery
    @PostMapping("/{id}/redeliver")
    public ResponseEntity<?> createRedelivery(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = auth.getName();
        UserEntity me = userRepo.findByEmail(email).orElse(null);
        if (!isAllowed(me)) return ResponseEntity.status(403).build();
        if (!me.getId().equals(c.getAssignedStaff().getId()) && !"ADMIN".equals(me.getRole()))
            return ResponseEntity.status(403).build();
        if (!"REDELIVER".equalsIgnoreCase(c.getDecisionType())) return ResponseEntity.badRequest().body("Decision is not REDELIVER");

        double total = 0.0;
        if (body != null && body.get("redeliveryTotal") != null) total = Double.valueOf(body.get("redeliveryTotal").toString());

        var original = c.getOrder();
        aptech.be.models.OrderEntity newOrder = new aptech.be.models.OrderEntity();
        newOrder.setCreatedAt(java.time.LocalDateTime.now());
        newOrder.setCustomer(original.getCustomer());
        newOrder.setOrderType("DELIVERY");
        newOrder.setDeliveryLatitude(original.getDeliveryLatitude());
        newOrder.setDeliveryLongitude(original.getDeliveryLongitude());
        newOrder.setDeliveryAddress(original.getDeliveryAddress());
        newOrder.setRecipientName(original.getRecipientName());
        newOrder.setRecipientPhone(original.getRecipientPhone());
        newOrder.setNote("Re-delivery for complaint #" + c.getId());
        newOrder.setTotalPrice(total);
        newOrder.setPaymentMethod("CASH");
        newOrder.setStatus("PREPARING");
        newOrder.setConfirmStatus("CONFIRMED");
        newOrder.setDeliveryStatus("PREPARING");

        // Clone foods with same quantities
        java.util.List<aptech.be.models.OrderFood> clonedFoods = new java.util.ArrayList<>();
        for (aptech.be.models.OrderFood of : original.getOrderFoods()) {
            aptech.be.models.OrderFood cof = new aptech.be.models.OrderFood();
            cof.setOrder(newOrder);
            cof.setFood(of.getFood());
            cof.setQuantity(of.getQuantity());
            clonedFoods.add(cof);
        }
        newOrder.setOrderFoods(clonedFoods);

        orderRepo.save(newOrder);
        c.setReDeliveryOrderId(newOrder.getId());
        complaintRepo.save(c);
        try { messagingTemplate.convertAndSend("/topic/complaints/" + c.getId(), java.util.Map.of("type","REDELIVERY_CREATED","caseId", c.getId(), "newOrderId", newOrder.getId())); } catch (Exception ignored) {}
        return ResponseEntity.ok(newOrder.getId());
    }
}


