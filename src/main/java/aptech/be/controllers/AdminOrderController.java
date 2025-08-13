package aptech.be.controllers;

import aptech.be.models.OrderEntity;
import aptech.be.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminOrderController {

    @Autowired
    private OrderRepository orderRepository;

    /**
     * Get all DINE-IN orders for admin
     */
    @GetMapping("/dine-in")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderEntity>> getDineInOrders() {
        try {
            List<OrderEntity> orders = orderRepository.findByOrderTypeOrderByCreatedAtDesc("DINE_IN");
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("[ADMIN ERROR] Failed to fetch dine-in orders: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all TAKE-AWAY orders for admin
     */
    @GetMapping("/take-away")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderEntity>> getTakeAwayOrders() {
        try {
            List<OrderEntity> orders = orderRepository.findByOrderTypeOrderByCreatedAtDesc("TAKE_AWAY");
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("[ADMIN ERROR] Failed to fetch take-away orders: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get specific order details for admin
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderEntity> getOrderDetails(@PathVariable Long orderId) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(orderOpt.get());
        } catch (Exception e) {
            System.err.println("[ADMIN ERROR] Failed to fetch order details: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get orders by status for admin
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderEntity>> getOrdersByStatus(@PathVariable String status) {
        try {
            List<OrderEntity> orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("[ADMIN ERROR] Failed to fetch orders by status: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get orders by staff for admin
     */
    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderEntity>> getOrdersByStaff(@PathVariable Long staffId) {
        try {
            List<OrderEntity> orders = orderRepository.findByStaffIdOrderByCreatedAtDesc(staffId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            System.err.println("[ADMIN ERROR] Failed to fetch orders by staff: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
} 