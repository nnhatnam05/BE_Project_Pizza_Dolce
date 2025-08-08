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
     * Helper method to find table by number
     */
    private TableEntity findTableByNumber(int tableNumber) {
        List<TableEntity> tables = tableService.getAllTables();
        return tables.stream()
                .filter(t -> t.getNumber() == tableNumber)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Table not found with number: " + tableNumber));
    }
    
    /**
     * Get table information by table number
     */
    @GetMapping("/table/{tableNumber}")
    public ResponseEntity<?> getTableByNumber(@PathVariable int tableNumber) {
        // Find table by number, not by ID
        try {
            System.out.println("=== DEBUG: Getting table by number ===");
            System.out.println("Table number: " + tableNumber);
            
            TableEntity table = findTableByNumber(tableNumber);
            
            System.out.println("Found table: " + table.getId() + " - " + table.getNumber());
            return ResponseEntity.ok(table);
        } catch (Exception e) {
            System.out.println("ERROR getting table: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Create a new dine-in order for a table
     */
    @PostMapping("/order")
    public ResponseEntity<?> createDineInOrder(@RequestBody OrderRequestDTO orderRequest) {
        try {
            System.out.println("=== DEBUG: Creating dine-in order ===");
            System.out.println("Request data: " + orderRequest);
            System.out.println("Table ID: " + orderRequest.getTableId());
            
            // Validate table exists and is available
            if (orderRequest.getTableId() == null) {
                System.out.println("ERROR: Table ID is null");
                return ResponseEntity.badRequest().body("Table ID is required for dine-in orders");
            }
            
            System.out.println("Looking for table with ID: " + orderRequest.getTableId());
            TableEntity table = tableService.getTableById(orderRequest.getTableId())
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            System.out.println("Found table: " + table.getId() + " - " + table.getNumber());
            
            // Create order with table reference
            OrderEntity order = orderService.createDineInOrder(orderRequest);
            
            // Update table status to occupied
            TableEntity updatedTable = tableService.updateTableStatus(table.getId(), TableService.STATUS_OCCUPIED);
            System.out.println("Table " + table.getNumber() + " status updated to: " + updatedTable.getStatus());
            
            System.out.println("Order created successfully: " + order.getId());
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            System.out.println("ERROR creating dine-in order: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating dine-in order: " + e.getMessage());
        }
    }
    
    /**
     * Get current order for a table by table number
     */
    @GetMapping("/table/{tableNumber}/current-order")
    public ResponseEntity<?> getCurrentOrderForTable(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Get active orders for this table using actual table ID
            List<OrderEntity> activeOrders = orderService.getActiveOrdersForTable(table.getId());
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
     * Add items to existing table order by table number (Always creates new order)
     */
    @PostMapping("/table/{tableNumber}/add-items")
    public ResponseEntity<?> addItemsToTableOrder(@PathVariable int tableNumber, 
                                                  @RequestBody OrderRequestDTO orderRequest) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Always create new order for each request
            OrderEntity newOrder = orderService.addItemsToTableOrder(table.getId(), orderRequest);
            return ResponseEntity.ok(newOrder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error adding items: " + e.getMessage());
        }
    }
    
    /**
     * Get all orders for a table by table number
     */
    @GetMapping("/table/{tableNumber}/all-orders")
    public ResponseEntity<?> getAllOrdersForTable(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Get active orders for this table (exclude PAID and COMPLETED)
            List<OrderEntity> allOrders = orderService.getActiveOrdersForTable(table.getId());
            System.out.println("Table " + tableNumber + " active orders count: " + allOrders.size());
            
            // Create detailed response with order items
            List<Map<String, Object>> detailedOrders = allOrders.stream()
                    .map(order -> {
                        Map<String, Object> orderMap = new java.util.HashMap<>();
                        orderMap.put("id", order.getId());
                        orderMap.put("orderNumber", order.getOrderNumber());
                        orderMap.put("status", order.getStatus());
                        orderMap.put("totalPrice", order.getTotalPrice());
                        orderMap.put("createdAt", order.getCreatedAt().toString());
                        
                        // Add detailed order items
                        List<Map<String, Object>> orderItemsDetails = order.getOrderItems().stream()
                                .map(item -> {
                                    Map<String, Object> itemMap = new java.util.HashMap<>();
                                    itemMap.put("foodId", item.getFood().getId());
                                    itemMap.put("foodName", item.getFood().getName());
                                    itemMap.put("foodPrice", item.getFood().getPrice());
                                    itemMap.put("quantity", item.getQuantity());
                                    itemMap.put("totalPrice", item.getTotalPrice());
                                    return itemMap;
                                })
                                .collect(java.util.stream.Collectors.toList());
                        
                        orderMap.put("orderItems", orderItemsDetails);
                        orderMap.put("orderItemsCount", order.getOrderItems().size());
                        
                        return orderMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(detailedOrders);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error getting orders: " + e.getMessage());
        }
    }
    
    /**
     * Get table summary with total quantities (UI display)
     */
    @GetMapping("/table/{tableNumber}/summary")
    public ResponseEntity<?> getTableSummary(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Get active orders and items summary (exclude PAID and COMPLETED)
            List<OrderEntity> allOrders = orderService.getActiveOrdersForTable(table.getId());
            
            // Calculate total amount to pay (sum of all order totals)
            double totalAmountToPay = allOrders.stream()
                    .mapToDouble(order -> order.getTotalPrice() != null ? order.getTotalPrice() : 0.0)
                    .sum();
            
            // Get detailed items summary with food names
            Map<String, Object> detailedItemsSummary = orderService.getDetailedTableItemsSummary(table.getId());
            
            // Create safe order representation without circular references
            List<Map<String, Object>> safeOrders = allOrders.stream()
                    .map(order -> {
                        Map<String, Object> orderMap = new java.util.HashMap<>();
                        orderMap.put("id", order.getId());
                        orderMap.put("orderNumber", order.getOrderNumber());
                        orderMap.put("status", order.getStatus());
                        orderMap.put("totalPrice", order.getTotalPrice());
                        orderMap.put("createdAt", order.getCreatedAt().toString());
                        orderMap.put("orderItemsCount", order.getOrderItems().size());
                        return orderMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "tableId", table.getId(),
                "tableNumber", table.getNumber(),
                "orders", safeOrders,
                "detailedItemsSummary", detailedItemsSummary,
                "totalOrders", allOrders.size(),
                "totalAmountToPay", totalAmountToPay
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Debug: print stack trace
            return ResponseEntity.badRequest().body("Error getting table summary: " + e.getMessage());
        }
    }
    
    /**
     * Debug API - Get table orders count only
     */
    @GetMapping("/table/{tableNumber}/debug")
    public ResponseEntity<?> debugTableOrders(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            // Get active orders count only (exclude PAID and COMPLETED)
            List<OrderEntity> allOrders = orderService.getActiveOrdersForTable(table.getId());
            
            return ResponseEntity.ok(Map.of(
                "tableId", table.getId(),
                "tableNumber", table.getNumber(),
                "totalOrders", allOrders.size()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Call staff for a table by table number
     */
    @PostMapping("/table/{tableNumber}/call-staff")
    public ResponseEntity<?> callStaff(@PathVariable int tableNumber, 
                                       @RequestBody Map<String, String> request) {
        try {
            System.out.println("=== STAFF CALL REQUEST ===");
            System.out.println("Table number: " + tableNumber);
            System.out.println("Request body: " + request);
            
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            System.out.println("Found table: " + table.getId() + " - " + table.getNumber());
            
            String reason = request.getOrDefault("reason", "General assistance");
            System.out.println("Call reason: " + reason);
            
            tableSessionService.createStaffCall(table.getId(), reason, table.getNumber());
            System.out.println("Staff call created successfully");
            
            return ResponseEntity.ok(Map.of("message", "Staff has been notified"));
        } catch (Exception e) {
            System.out.println("ERROR in staff call: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error calling staff: " + e.getMessage());
        }
    }
    
    /**
     * Request payment for a table by table number
     */
    @PostMapping("/table/{tableNumber}/request-payment")
    public ResponseEntity<?> requestPayment(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            tableSessionService.createPaymentRequest(table.getId(), table.getNumber());
            return ResponseEntity.ok(Map.of("message", "Payment request sent to staff"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error requesting payment: " + e.getMessage());
        }
    }
    
    /**
     * Get table session info by table number
     */
    @GetMapping("/table/{tableNumber}/session")
    public ResponseEntity<?> getTableSession(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            Map<String, Object> sessionInfo = tableSessionService.getTableSessionInfo(table.getId());
            return ResponseEntity.ok(sessionInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting session info: " + e.getMessage());
        }
    }

    @PostMapping("/table/{tableNumber}/end-session")
    public ResponseEntity<?> endTableSession(@PathVariable int tableNumber) {
        try {
            // Find table by number first
            List<TableEntity> tables = tableService.getAllTables();
            TableEntity table = tables.stream()
                    .filter(t -> t.getNumber() == tableNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Table not found"));
            
            tableSessionService.endSession(table.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Table session ended successfully",
                "tableNumber", tableNumber,
                "tableId", table.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error ending table session: " + e.getMessage());
        }
    }
} 