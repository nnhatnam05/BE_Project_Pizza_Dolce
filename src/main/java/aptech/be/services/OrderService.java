package aptech.be.services;

import aptech.be.dto.OrderRequestDTO;
import aptech.be.dto.FoodOrderItemDTO;
import aptech.be.models.*;
import aptech.be.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private FoodRepository foodRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private TableService tableService;
    
    @Autowired
    private WebSocketNotificationService notificationService;
    
    @Autowired
    private OrderItemsRepository orderItemsRepository;
    
    // Order types
    public static final String ORDER_TYPE_DELIVERY = "DELIVERY";
    public static final String ORDER_TYPE_DINE_IN = "DINE_IN";
    
    // Order statuses for dine-in
    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_SERVED = "SERVED";
    public static final String STATUS_WAITING_PAYMENT = "WAITING_PAYMENT";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_COMPLETED = "COMPLETED";
    
    @Transactional
    public OrderEntity createDineInOrder(OrderRequestDTO orderRequest) {
        // Validate table exists
        TableEntity table = tableService.getTableById(orderRequest.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));
        
        // Create order entity
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(ORDER_TYPE_DINE_IN);
        order.setTable(table);
        order.setStatus(STATUS_NEW);
        order.setCreatedAt(LocalDateTime.now());
        order.setNote(orderRequest.getNote());
        order.setVoucherCode(orderRequest.getVoucherCode());
        order.setVoucherDiscount(orderRequest.getVoucherDiscount());
        order.setNeedInvoice(orderRequest.getNeedInvoice());
        
        // Set customer if provided (optional for dine-in)
        if (orderRequest.getCustomerId() != null) {
            Customer customer = customerRepository.findById(orderRequest.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            order.setCustomer(customer);
        }
        
        // Calculate total price and create order foods
        double totalPrice = 0.0;
        for (FoodOrderItemDTO foodItem : orderRequest.getFoods()) {
            Food food = foodRepository.findById(foodItem.getId())
                    .orElseThrow(() -> new RuntimeException("Food not found: " + foodItem.getId()));
            
            OrderFood orderFood = new OrderFood();
            orderFood.setFood(food);
            orderFood.setQuantity(foodItem.getQuantity());
            orderFood.setOrder(order);
            
            order.getOrderFoods().add(orderFood);
            totalPrice += food.getPrice() * foodItem.getQuantity();
        }
        
        // Apply voucher discount
        if (orderRequest.getVoucherDiscount() != null) {
            totalPrice -= orderRequest.getVoucherDiscount();
        }
        
        order.setTotalPrice(totalPrice);
        
        OrderEntity savedOrder = orderRepository.save(order);
        
        // Send real-time notification to staff
        notificationService.sendNewOrderNotification(savedOrder);
        
        return savedOrder;
    }
    
    public List<OrderEntity> getActiveOrdersForTable(Long tableId) {
        return orderRepository.findByTableIdAndStatusNotIn(tableId, 
                List.of(STATUS_COMPLETED, STATUS_PAID));
    }
    
    @Transactional
    public OrderEntity addItemsToTableOrder(Long tableId, OrderRequestDTO orderRequest) {
        // Always create new order for each dine-in request
            orderRequest.setTableId(tableId);
        return createDineInOrderWithItems(orderRequest);
    }
    
    public OrderEntity updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        OrderEntity updatedOrder = orderRepository.save(order);
        
        // Send real-time notification to staff about status update
        notificationService.sendOrderStatusUpdateNotification(updatedOrder, status);
        
        return updatedOrder;
    }
    
    public List<OrderEntity> getOrdersByTable(Long tableId) {
        return orderRepository.findByTableIdOrderByCreatedAtDesc(tableId);
    }
    
    public List<OrderEntity> getDineInOrdersByStatus(String status) {
        return orderRepository.findByOrderTypeAndStatus(ORDER_TYPE_DINE_IN, status);
    }
    
    public List<OrderEntity> getAllDineInOrders() {
        return orderRepository.findByOrderType(ORDER_TYPE_DINE_IN);
    }
    
    // New methods for table order management
    public List<OrderEntity> getAllOrdersForTable(Long tableId) {
        return orderRepository.findByTableIdOrderByCreatedAtDesc(tableId);
    }
    
    public List<OrderItems> getAllOrderItemsForTable(Long tableId) {
        return orderItemsRepository.findByTableId(tableId);
    }
    
    public List<OrderItems> getActiveOrderItemsForTable(Long tableId) {
        List<String> excludedStatuses = List.of(STATUS_COMPLETED, STATUS_PAID);
        return orderItemsRepository.findByTableIdAndOrderStatusNotIn(tableId, excludedStatuses);
    }
    
    // Get summary of all items for a table (for UI display)
    public Map<Long, Integer> getTableItemsSummary(Long tableId) {
        List<String> excludedStatuses = List.of(STATUS_COMPLETED, STATUS_PAID);
        List<Object[]> results = orderItemsRepository.findTotalQuantityByFoodAndTableId(tableId, excludedStatuses);
        
        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                    result -> (Long) result[0],  // food_id
                    result -> ((Long) result[1]).intValue()  // total_quantity
                ));
    }
    
    // Get detailed summary with food names for a table (for UI display)
    public Map<String, Object> getDetailedTableItemsSummary(Long tableId) {
        List<String> excludedStatuses = List.of(STATUS_COMPLETED, STATUS_PAID);
        List<OrderItems> orderItems = orderItemsRepository.findByTableIdAndOrderStatusNotIn(tableId, excludedStatuses);
        
        // Group by food and sum quantities
        Map<Food, Integer> foodQuantityMap = orderItems.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    OrderItems::getFood,
                    java.util.stream.Collectors.summingInt(OrderItems::getQuantity)
                ));
        
        // Convert to detailed summary
        List<Map<String, Object>> itemsList = foodQuantityMap.entrySet().stream()
                .map(entry -> {
                    Food food = entry.getKey();
                    Integer totalQuantity = entry.getValue();
                    
                    Map<String, Object> itemSummary = new java.util.HashMap<>();
                    itemSummary.put("foodId", food.getId());
                    itemSummary.put("foodName", food.getName());
                    itemSummary.put("foodPrice", food.getPrice());
                    itemSummary.put("totalQuantity", totalQuantity);
                    itemSummary.put("totalPrice", food.getPrice() * totalQuantity);
                    
                    return itemSummary;
                })
                .sorted((a, b) -> ((String) a.get("foodName")).compareTo((String) b.get("foodName")))
                .collect(java.util.stream.Collectors.toList());
        
        // Calculate totals
        int totalItems = itemsList.stream()
                .mapToInt(item -> (Integer) item.get("totalQuantity"))
                .sum();
        
        double totalValue = itemsList.stream()
                .mapToDouble(item -> (Double) item.get("totalPrice"))
                .sum();
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("items", itemsList);
        result.put("totalItems", totalItems);
        result.put("totalValue", totalValue);
        
        return result;
    }
    
    @Transactional
    public OrderEntity createDineInOrderWithItems(OrderRequestDTO orderRequest) {
        // Create new order
        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(ORDER_TYPE_DINE_IN);
        order.setStatus(STATUS_NEW);
        order.setCreatedAt(LocalDateTime.now());
        order.setNote(orderRequest.getNote());
        order.setVoucherCode(orderRequest.getVoucherCode());
        order.setVoucherDiscount(orderRequest.getVoucherDiscount());
        order.setNeedInvoice(orderRequest.getNeedInvoice());
        
        // Set table
        TableEntity table = tableService.getTableById(orderRequest.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));
        order.setTable(table);
        
        // Set customer if provided (optional for dine-in)
        if (orderRequest.getCustomerId() != null) {
            Customer customer = customerRepository.findById(orderRequest.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            order.setCustomer(customer);
        }
        
        // Calculate total price and create order items
        double totalPrice = 0.0;
        for (FoodOrderItemDTO foodItem : orderRequest.getFoods()) {
            Food food = foodRepository.findById(foodItem.getId())
                    .orElseThrow(() -> new RuntimeException("Food not found: " + foodItem.getId()));
            
            OrderItems orderItems = new OrderItems(order, food, foodItem.getQuantity());
            
            order.getOrderItems().add(orderItems);
            totalPrice += orderItems.getTotalPrice();
        }
        
        // Apply voucher discount
        if (orderRequest.getVoucherDiscount() != null) {
            totalPrice -= orderRequest.getVoucherDiscount();
        }
        
        order.setTotalPrice(totalPrice);
        
        OrderEntity savedOrder = orderRepository.save(order);
        
        // Send real-time notification to staff
        notificationService.sendNewOrderNotification(savedOrder);
        
        return savedOrder;
    }
    
    private String generateOrderNumber() {
        return "DIN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
} 