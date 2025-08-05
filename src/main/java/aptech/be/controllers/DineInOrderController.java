package aptech.be.controllers;

import aptech.be.dto.OrderRequestDTO;
import aptech.be.models.OrderEntity;
import aptech.be.models.TableEntity;
import aptech.be.services.OrderService;
import aptech.be.services.TableService;
import aptech.be.services.TableSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/dinein")
@CrossOrigin(origins = "http://localhost:3000")
public class DineInOrderController {
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TableService tableService;
    
    @Autowired
    private TableSessionService tableSessionService;
    
    /**
     * Get table information by table number
     */
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<?> getTableByNumber(@PathVariable int tableNumber) {
        // Find table by number, not by ID
        try {
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            return ResponseEntity.ok(table);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create a new dine-in order for a table
     */
    @PostMapping("/order")
    public ResponseEntity<?> createDineInOrder(@RequestBody OrderRequestDTO orderRequest) {
        try {
            // Validate table exists and is available
            if (orderRequest.getTableId() == null) {
                return ResponseEntity.badRequest().body("Table ID is required for dine-in orders");
            }
            
            TableEntity table = tableService.getTableById(orderRequest.getTableId())
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Create order with table reference
            OrderEntity order = orderService.createDineInOrder(orderRequest);
            
            // Update table status to occupied
            tableService.updateTableStatus(table.getId(), TableService.STATUS_OCCUPIED);
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating dine-in order: " + e.getMessage());
        }
    }
    
    /**
     * Get current order for a table
     */
    @GetMapping("/table/{tableId}/current-order")
    public ResponseEntity<?> getCurrentOrderForTable(@PathVariable Long tableId) {
        try {
            List<OrderEntity> activeOrders = orderService.getActiveOrdersForTable(tableId);
            if (activeOrders.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasActiveOrder", false));
            }
            
            // Return the most recent active order
            OrderEntity currentOrder = activeOrders.get(0);
            return ResponseEntity.ok(Map.of(
                "hasActiveOrder", true,
                "order", currentOrder
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting current order: " + e.getMessage());
        }
    }
    
    /**
     * Add items to existing table order
     */
    @PostMapping("/table/{tableId}/add-items")
    public ResponseEntity<?> addItemsToTableOrder(@PathVariable Long tableId, 
                                                  @RequestBody OrderRequestDTO orderRequest) {
        try {
            OrderEntity updatedOrder = orderService.addItemsToTableOrder(tableId, orderRequest);
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding items: " + e.getMessage());
        }
    }
    
    /**
     * Call staff for a table
     */
    @PostMapping("/table/{tableId}/call-staff")
    public ResponseEntity<?> callStaff(@PathVariable Long tableId, 
                                       @RequestBody Map<String, String> request) {
        try {
            String reason = request.getOrDefault("reason", "General assistance");
            tableSessionService.createStaffCall(tableId, reason);
            return ResponseEntity.ok(Map.of("message", "Staff has been notified"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error calling staff: " + e.getMessage());
        }
    }
    
    /**
     * Request payment for a table
     */
    @PostMapping("/table/{tableId}/request-payment")
    public ResponseEntity<?> requestPayment(@PathVariable Long tableId) {
        try {
            tableSessionService.createPaymentRequest(tableId);
            return ResponseEntity.ok(Map.of("message", "Payment request sent to staff"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error requesting payment: " + e.getMessage());
        }
    }
    
    /**
     * Get table session info
     */
    @GetMapping("/table/{tableId}/session")
    public ResponseEntity<?> getTableSession(@PathVariable Long tableId) {
        try {
            Map<String, Object> sessionInfo = tableSessionService.getTableSessionInfo(tableId);
            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting session info: " + e.getMessage());
        }
    }

    @PostMapping("/table/{tableId}/end-session")
    public ResponseEntity<?> endTableSession(@PathVariable Long tableId) {
        try {
            tableSessionService.endSession(tableId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Table session ended successfully",
                "tableId", tableId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error ending table session: " + e.getMessage());
        }
    }
} 