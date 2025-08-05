package aptech.be.controllers.shipper;

import aptech.be.dto.shipper.ShipperDTO;
import aptech.be.dto.shipper.DeliveryUpdateDTO;
import aptech.be.dto.shipper.LocationUpdateDTO;
import aptech.be.models.OrderEntity;
import aptech.be.models.UserEntity;
import aptech.be.repositories.UserRepository;
import aptech.be.services.shipper.ShipperService;
import aptech.be.services.shipper.OrderAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipper")
@PreAuthorize("hasRole('SHIPPER')")
@CrossOrigin(origins = "http://localhost:3000")
public class ShipperController {

    @Autowired
    private ShipperService shipperService;

    @Autowired
    private OrderAssignmentService orderAssignmentService;

    @Autowired
    private UserRepository userRepository;

    // Lấy thông tin profile
    @GetMapping("/profile")
    public ResponseEntity<ShipperDTO> getProfile(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        ShipperDTO profile = shipperService.getShipperProfile(userId);
        return ResponseEntity.ok(profile);
    }

    // Cập nhật vị trí
    @PutMapping("/location")
    public ResponseEntity<?> updateLocation(@RequestBody LocationUpdateDTO request, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        shipperService.updateLocation(userId, request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok(Map.of("message", "Location updated successfully"));
    }

    // Cập nhật trạng thái
    @PutMapping("/status")
    public ResponseEntity<?> updateStatus(@RequestBody Map<String, String> request, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        String status = request.get("status");
        shipperService.updateStatus(userId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated successfully"));
    }

    // Lấy danh sách đơn hàng được gán
    @GetMapping("/orders")
    public ResponseEntity<List<OrderEntity>> getMyOrders(
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<OrderEntity> orders = orderAssignmentService.getShipperOrders(userId, status);
        return ResponseEntity.ok(orders);
    }

    // Nhận đơn hàng
    @PutMapping("/orders/{orderId}/accept")
    public ResponseEntity<?> acceptOrder(@PathVariable Long orderId, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        orderAssignmentService.acceptOrder(orderId, userId);
        return ResponseEntity.ok(Map.of("message", "Order accepted successfully"));
    }

    // Cập nhật trạng thái giao hàng
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        String status = request.get("status");
        orderAssignmentService.updateOrderStatus(orderId, userId, status);
        return ResponseEntity.ok(Map.of("message", "Order status updated successfully"));
    }

    // Từ chối đơn hàng
    @PutMapping("/orders/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        String reason = request.get("reason");
        orderAssignmentService.rejectOrder(orderId, userId, reason);
        return ResponseEntity.ok(Map.of("message", "Order rejected successfully"));
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() != null) {
            String username = authentication.getName();
            // Tìm user theo username
            return userRepository.findByUsername(username)
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        throw new RuntimeException("Authentication required");
    }
}