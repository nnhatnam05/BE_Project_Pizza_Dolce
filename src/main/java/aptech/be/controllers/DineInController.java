package aptech.be.controllers;

import aptech.be.models.OrderEntity;
import aptech.be.repositories.OrderRepository;
import aptech.be.services.TableSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dinein")
@CrossOrigin(origins = "http://localhost:3000")
public class DineInController {

    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private TableSessionService tableSessionService;

    @GetMapping("/sessions/all")
    public ResponseEntity<?> getAllSessions() {
        try {
            Map<String, Object> sessions = new HashMap<>();
            
            // Get staff calls and payment requests from TableSessionService
            Map<Long, TableSessionService.StaffCall> staffCallsMap = tableSessionService.getAllPendingStaffCalls();
            Map<Long, TableSessionService.PaymentRequest> paymentRequestsMap = tableSessionService.getAllPendingPaymentRequests();
            
            // Convert to format expected by frontend
            List<Map<String, Object>> staffCalls = staffCallsMap.values().stream()
                    .map(call -> {
                        Map<String, Object> callMap = new HashMap<>();
                        callMap.put("tableId", call.getTableId());
                        callMap.put("message", call.getReason());
                        callMap.put("timestamp", call.getCallTime().toString());
                        return callMap;
                    })
                    .collect(Collectors.toList());
                    
            List<Map<String, Object>> paymentRequests = paymentRequestsMap.values().stream()
                    .map(request -> {
                        Map<String, Object> requestMap = new HashMap<>();
                        requestMap.put("tableId", request.getTableId());
                        requestMap.put("timestamp", request.getRequestTime().toString());
                        return requestMap;
                    })
                    .collect(Collectors.toList());
            
            sessions.put("staffCalls", staffCalls);
            sessions.put("paymentRequests", paymentRequests);
            
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching sessions: " + e.getMessage());
        }
    }

    @PostMapping("/staff-calls/{tableId}/resolve")
    public ResponseEntity<?> resolveStaffCall(@PathVariable Long tableId) {
        try {
            tableSessionService.resolveStaffCall(tableId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Staff call resolved successfully",
                "tableId", tableId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resolving staff call: " + e.getMessage());
        }
    }

    @PostMapping("/payment-requests/{tableId}/resolve")
    public ResponseEntity<?> resolvePaymentRequest(@PathVariable Long tableId) {
        try {
            tableSessionService.resolvePaymentRequest(tableId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Payment request resolved successfully",
                "tableId", tableId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error resolving payment request: " + e.getMessage());
        }
    }

    // Keep existing methods for order management
    @GetMapping("/orders/all")
    public ResponseEntity<?> getAllOrders() {
        try {
            List<OrderEntity> orders = orderRepository.findAll();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching orders: " + e.getMessage());
        }
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get("status");
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Status is required");
            }

            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            order.setStatus(newStatus.toUpperCase());
            
            orderRepository.save(order);
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating order status: " + e.getMessage());
        }
    }

    // Debug endpoints for testing
    @PostMapping("/debug/add-staff-call/{tableId}")
    public ResponseEntity<?> debugAddStaffCall(@PathVariable Long tableId) {
        tableSessionService.createStaffCall(tableId, "Customer needs assistance");
        return ResponseEntity.ok("Staff call added for table " + tableId);
    }

    @PostMapping("/debug/add-payment-request/{tableId}")
    public ResponseEntity<?> debugAddPaymentRequest(@PathVariable Long tableId) {
        tableSessionService.createPaymentRequest(tableId);
        return ResponseEntity.ok("Payment request added for table " + tableId);
    }
} 