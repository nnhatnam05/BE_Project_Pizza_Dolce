package aptech.be.controllers.shipper;

import aptech.be.dto.shipper.OrderAssignmentDTO;
import aptech.be.dto.shipper.ShipperDTO;
import aptech.be.services.shipper.OrderAssignmentService;
import aptech.be.services.shipper.ShipperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/shipper")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminShipperController {

    @Autowired
    private ShipperService shipperService;

    @Autowired
    private OrderAssignmentService orderAssignmentService;

    // Lấy danh sách tất cả shipper
    @GetMapping
    public ResponseEntity<List<ShipperDTO>> getAllShippers() {
        List<ShipperDTO> shippers = shipperService.getAvailableShippers();
        return ResponseEntity.ok(shippers);
    }

    // Gán đơn hàng cho shipper
    @PostMapping("/assign-order")
    public ResponseEntity<?> assignOrder(@RequestBody OrderAssignmentDTO request) {
        orderAssignmentService.assignOrderToShipper(request.getOrderId(), request.getShipperId());
        return ResponseEntity.ok(Map.of("message", "Order assigned successfully"));
    }

    // Tự động gán đơn hàng
    @PostMapping("/auto-assign/{orderId}")
    public ResponseEntity<?> autoAssignOrder(@PathVariable Long orderId) {
        orderAssignmentService.autoAssignOrder(orderId);
        return ResponseEntity.ok(Map.of("message", "Order auto-assigned successfully"));
    }

    // Quản lý shipper
    @PutMapping("/{shipperId}/status")
    public ResponseEntity<?> updateShipperStatus(@PathVariable Long shipperId, @RequestParam String status) {
        shipperService.updateStatus(shipperId, status);
        return ResponseEntity.ok(Map.of("message", "Shipper status updated successfully"));
    }
}
