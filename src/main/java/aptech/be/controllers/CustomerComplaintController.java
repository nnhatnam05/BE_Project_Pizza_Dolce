package aptech.be.controllers;

import aptech.be.controllers.admin.ComplaintAdminController;
import aptech.be.models.ComplaintAttachment;
import aptech.be.models.ComplaintCase;
import aptech.be.models.ComplaintMessage;
import aptech.be.models.Customer;
import aptech.be.models.OrderEntity;
import aptech.be.repositories.ComplaintCaseRepository;
import aptech.be.repositories.ComplaintMessageRepository;
import aptech.be.repositories.CustomerRepository;
import aptech.be.repositories.OrderRepository;
import aptech.be.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000")
public class CustomerComplaintController {

    @Autowired private ComplaintCaseRepository complaintRepo;
    @Autowired private OrderRepository orderRepo;
    @Autowired private CustomerRepository customerRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ComplaintMessageRepository messageRepo;
    @Autowired private SimpMessagingTemplate messagingTemplate;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createComplaint(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        String type = String.valueOf(body.getOrDefault("type", "REFUND_REQUEST"));
        String reason = String.valueOf(body.getOrDefault("reason", ""));

        OrderEntity order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");

        String email = jwt != null ? jwt.getSubject() : (auth != null ? auth.getName() : null);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        Customer customer = customerRepo.findByEmail(email).orElse(null);
        if (customer == null || !order.getCustomer().getId().equals(customer.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        // Eligibility: delivered within 1 hour
        if (order.getDeliveredAt() == null || order.getDeliveryStatus() == null || !"DELIVERED".equals(order.getDeliveryStatus())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Order not delivered");
        }
        if (Duration.between(order.getDeliveredAt(), LocalDateTime.now()).toMinutes() > 60) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Complaint window expired");
        }

        ComplaintCase c = new ComplaintCase();
        c.setOrder(order);
        c.setCustomer(customer);
        c.setType(type);
        c.setReason(reason);
        c.setStatus("OPEN");
        c.setDeliveredAtSnapshot(order.getDeliveredAt());
        c.setAutoDecisionEnabledSnapshot(ComplaintAdminController.isAutoDecisionEnabled());

        Long staffId = ComplaintAdminController.getAssignedSupportStaffId();
        if (staffId != null) {
            userRepo.findById(staffId).ifPresent(c::setAssignedStaff);
        }
        complaintRepo.save(c);
        return ResponseEntity.ok(c.getId());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ComplaintCase> myComplaints(Authentication auth) {
        String email = auth.getName();
        Customer customer = customerRepo.findByEmail(email).orElse(null);
        return complaintRepo.findByCustomerId(customer.getId());
    }

    @GetMapping("/{id}/messages")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMessages(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return ResponseEntity.ok(messageRepo.findByComplaintIdOrderByCreatedAtAsc(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getComplaint(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(c);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> postMessage(@PathVariable Long id, @RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        var c = complaintRepo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String email = jwt != null ? jwt.getSubject() : (auth != null ? auth.getName() : null);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Customer me = customerRepo.findByEmail(email).orElse(null);
        if (!me.getId().equals(c.getCustomer().getId())) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        ComplaintMessage m = new ComplaintMessage();
        m.setComplaint(c);
        m.setSenderType("CUSTOMER");
        m.setSenderId(me.getId());
        m.setMessage(String.valueOf(body.getOrDefault("message", "")));
        messageRepo.save(m);
        messagingTemplate.convertAndSend("/topic/complaints/" + id, m);
        return ResponseEntity.ok(m.getId());
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    public ResponseEntity<?> uploadAttachment(@PathVariable Long id, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt, Authentication auth) {
        try {
            var c = complaintRepo.findById(id).orElse(null);
            if (c == null) return ResponseEntity.notFound().build();
            if (file.isEmpty()) return ResponseEntity.badRequest().body("Empty file");
            Path dir = Paths.get("uploads", "complaints", String.valueOf(id));
            Files.createDirectories(dir);
            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ComplaintAttachment att = new ComplaintAttachment();
            att.setComplaint(c);
            att.setUrl("/uploads/complaints/" + id + "/" + filename);
            att.setMimeType(file.getContentType());
            Object uid = null;
            String email = jwt != null ? jwt.getSubject() : (auth != null ? auth.getName() : null);
            if (email != null) {
                var user = userRepo.findByEmail(email);
                uid = user.map(u -> u.getId()).orElse(c.getCustomer().getId());
            }
            if (uid != null) att.setUploadedBy(Long.valueOf(uid.toString()));
            c.getAttachments().add(att);
            complaintRepo.save(c);
            messagingTemplate.convertAndSend("/topic/complaints/" + id, att);
            return ResponseEntity.ok(att.getUrl());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}


