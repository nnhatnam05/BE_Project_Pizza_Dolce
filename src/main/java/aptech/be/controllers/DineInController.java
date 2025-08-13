package aptech.be.controllers;

import aptech.be.models.ClaimToken;
import aptech.be.models.OrderEntity;
import aptech.be.models.OrderItems;
import aptech.be.models.Food;
import aptech.be.models.TableEntity;
import aptech.be.repositories.OrderRepository;
import aptech.be.repositories.OrderItemsRepository;
import aptech.be.repositories.FoodRepository;
import aptech.be.repositories.TableRepository;
import aptech.be.services.ClaimTokenService;
import aptech.be.services.TableSessionService;
import aptech.be.services.OrderService;
import aptech.be.services.WebSocketNotificationService;
import aptech.be.models.UserEntity;
import aptech.be.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private OrderItemsRepository orderItemsRepository;
    
    @Autowired
    private FoodRepository foodRepository;
    
    @Autowired
    private TableRepository tableRepository;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private TableSessionService tableSessionService;
    
    @Autowired
    private ClaimTokenService claimTokenService;
    
    @Autowired
    private WebSocketNotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;

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

    /**
     * Get detailed bill for a table (for payment confirmation)
     */
    @GetMapping("/tables/{tableId}/bill")
    public ResponseEntity<?> getTableBill(@PathVariable Long tableId) {
        try {
            // Get table
            Optional<TableEntity> tableOpt = tableRepository.findById(tableId);
            if (!tableOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            TableEntity table = tableOpt.get();
            
            // Get all active orders for this table
            List<OrderEntity> activeOrders = orderService.getActiveOrdersForTable(tableId);
            
            if (activeOrders.isEmpty()) {
                return ResponseEntity.badRequest().body("No active orders found for this table");
            }
            
            // Calculate total bill
            double totalAmount = 0.0;
            List<Map<String, Object>> billItems = new ArrayList<>();
            
            for (OrderEntity order : activeOrders) {
                for (OrderItems item : order.getOrderItems()) {
                    Map<String, Object> billItem = new HashMap<>();
                    billItem.put("foodName", item.getFood().getName());
                    billItem.put("unitPrice", item.getUnitPrice());
                    billItem.put("quantity", item.getQuantity());
                    billItem.put("totalPrice", item.getTotalPrice());
                    billItem.put("orderNumber", order.getOrderNumber());
                    billItems.add(billItem);
                    
                    totalAmount += item.getTotalPrice();
                }
            }
            
            Map<String, Object> bill = new HashMap<>();
            bill.put("tableId", tableId);
            bill.put("tableNumber", table.getNumber());
            bill.put("items", billItems);
            bill.put("totalAmount", totalAmount);
            bill.put("ordersCount", activeOrders.size());
            bill.put("itemsCount", billItems.size());
            bill.put("generatedAt", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating bill: " + e.getMessage());
        }
    }

    /**
     * Confirm payment for a table - mark all orders as PAID and reset table status
     */
    @PostMapping("/tables/{tableId}/confirm-payment")
    public ResponseEntity<?> confirmTablePayment(@PathVariable Long tableId) {
        try {
            // Get table
            Optional<TableEntity> tableOpt = tableRepository.findById(tableId);
            if (!tableOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            TableEntity table = tableOpt.get();
            
            // Get all active orders for this table
            List<OrderEntity> activeOrders = orderService.getActiveOrdersForTable(tableId);
            
            if (activeOrders.isEmpty()) {
                return ResponseEntity.badRequest().body("No active orders found for this table");
            }
            
            // Mark all orders as PAID
            for (OrderEntity order : activeOrders) {
                order.setStatus("PAID");
                orderRepository.save(order);
            }
            
            // Reset table status to AVAILABLE
            table.setStatus("AVAILABLE");
            tableRepository.save(table);
            
            // Clear table session (remove staff calls and payment requests)
            tableSessionService.endSession(tableId);
            
            // NEW: Create claim token for point claiming
            ClaimToken claimToken = claimTokenService.createClaimToken(activeOrders);
            
            // Send WebSocket notification to customer with claim token
            try {
                notificationService.sendPaymentConfirmedNotification(table.getNumber(), claimToken.getToken());
            } catch (Exception e) {
                System.err.println("[WEBSOCKET ERROR] Failed to send payment confirmation: " + e.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Payment confirmed successfully",
                "tableId", tableId,
                "tableNumber", table.getNumber(),
                "ordersUpdated", activeOrders.size(),
                "claimToken", claimToken.getToken(),
                "pointsToEarn", claimToken.getPointsToEarn(),
                "totalAmount", claimToken.getTotalAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming payment: " + e.getMessage());
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
            
            // Get current staff user who is updating the status
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUserEmail = authentication.getName();
            
            Optional<UserEntity> staffOpt = userRepository.findByEmail(currentUserEmail);
            if (staffOpt.isPresent()) {
                UserEntity staff = staffOpt.get();
                if ("STAFF".equals(staff.getRole()) || "ADMIN".equals(staff.getRole())) {
                    // Set staff who is updating the order (for DINE-IN, staff updates status)
                    order.setStaff(staff);
                }
            }
            
            order.setStatus(newStatus.toUpperCase());
            order.setUpdatedAt(LocalDateTime.now());
            
            orderRepository.save(order);
            
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating order status: " + e.getMessage());
        }
    }

    /**
     * Get single order details with items
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            
            // Create detailed response with order items
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("status", order.getStatus());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("createdAt", order.getCreatedAt());
            
            // Add table info
            if (order.getTable() != null) {
                Map<String, Object> tableMap = new HashMap<>();
                tableMap.put("id", order.getTable().getId());
                tableMap.put("number", order.getTable().getNumber());
                orderMap.put("table", tableMap);
            }
            
            // Add detailed order items
            List<Map<String, Object>> orderItemsDetails = order.getOrderItems().stream()
                    .map(item -> {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", item.getId());
                        itemMap.put("quantity", item.getQuantity());
                        itemMap.put("totalPrice", item.getTotalPrice());
                        itemMap.put("foodName", item.getFood().getName());
                        itemMap.put("foodPrice", item.getFood().getPrice());
                        
                        // Add food details
                        Map<String, Object> foodMap = new HashMap<>();
                        foodMap.put("id", item.getFood().getId());
                        foodMap.put("name", item.getFood().getName());
                        foodMap.put("price", item.getFood().getPrice());
                        itemMap.put("food", foodMap);
                        
                        return itemMap;
                    })
                    .collect(Collectors.toList());
            
            orderMap.put("orderItems", orderItemsDetails);
            orderMap.put("orderItemsCount", order.getOrderItems().size());
            
            return ResponseEntity.ok(orderMap);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching order details: " + e.getMessage());
        }
    }

    /**
     * Add item to existing order (only if status = NEW)
     */
    @PostMapping("/orders/{orderId}/items")
    public ResponseEntity<?> addOrderItem(
            @PathVariable Long orderId,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            
            // Check if order can be edited
            if (!"NEW".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Order cannot be edited. Only orders with NEW status can be modified.");
            }

            Long foodId = Long.valueOf(request.get("foodId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());

            Optional<Food> foodOpt = foodRepository.findById(foodId);
            if (!foodOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Food not found");
            }

            Food food = foodOpt.get();
            
            // Create new order item using constructor
            OrderItems orderItem = new OrderItems(order, food, quantity);

            orderItemsRepository.save(orderItem);
            
            // Update order total price
            updateOrderTotalPrice(order);

            return ResponseEntity.ok(Map.of("message", "Item added successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding item: " + e.getMessage());
        }
    }

    /**
     * Update order item quantity (only if order status = NEW)
     */
    @PutMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<?> updateOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            
            // Check if order can be edited
            if (!"NEW".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Order cannot be edited. Only orders with NEW status can be modified.");
            }

            Optional<OrderItems> itemOpt = orderItemsRepository.findById(itemId);
            if (!itemOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderItems orderItem = itemOpt.get();
            
            // Verify item belongs to this order
            if (!orderItem.getOrder().getId().equals(orderId)) {
                return ResponseEntity.badRequest().body("Item does not belong to this order");
            }

            Integer newQuantity = Integer.valueOf(request.get("quantity").toString());
            
            if (newQuantity <= 0) {
                return ResponseEntity.badRequest().body("Quantity must be greater than 0");
            }

            orderItem.setQuantity(newQuantity);
            orderItem.setTotalPrice(orderItem.getFood().getPrice() * newQuantity);

            orderItemsRepository.save(orderItem);
            
            // Update order total price
            updateOrderTotalPrice(order);

            return ResponseEntity.ok(Map.of("message", "Item updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating item: " + e.getMessage());
        }
    }

    /**
     * Delete order item (only if order status = NEW)
     */
    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<?> deleteOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        try {
            Optional<OrderEntity> orderOpt = orderRepository.findById(orderId);
            if (!orderOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderEntity order = orderOpt.get();
            
            // Check if order can be edited
            if (!"NEW".equals(order.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Order cannot be edited. Only orders with NEW status can be modified.");
            }

            Optional<OrderItems> itemOpt = orderItemsRepository.findById(itemId);
            if (!itemOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            OrderItems orderItem = itemOpt.get();
            
            // Verify item belongs to this order
            if (!orderItem.getOrder().getId().equals(orderId)) {
                return ResponseEntity.badRequest().body("Item does not belong to this order");
            }

            orderItemsRepository.delete(orderItem);
            
            // Update order total price
            updateOrderTotalPrice(order);

            return ResponseEntity.ok(Map.of("message", "Item deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting item: " + e.getMessage());
        }
    }

    /**
     * Helper method to update order total price
     */
    private void updateOrderTotalPrice(OrderEntity order) {
        List<OrderItems> items = orderItemsRepository.findByOrderId(order.getId());
        double totalPrice = items.stream()
                .mapToDouble(OrderItems::getTotalPrice)
                .sum();
        order.setTotalPrice(totalPrice);
        orderRepository.save(order);
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