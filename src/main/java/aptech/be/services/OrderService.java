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
        
        return orderRepository.save(order);
    }
    
    public List<OrderEntity> getActiveOrdersForTable(Long tableId) {
        return orderRepository.findByTableIdAndStatusNotIn(tableId, 
                List.of(STATUS_COMPLETED, STATUS_PAID));
    }
    
    @Transactional
    public OrderEntity addItemsToTableOrder(Long tableId, OrderRequestDTO orderRequest) {
        // Get the most recent active order for the table
        List<OrderEntity> activeOrders = getActiveOrdersForTable(tableId);
        
        if (activeOrders.isEmpty()) {
            // Create new order if no active order exists
            orderRequest.setTableId(tableId);
            return createDineInOrder(orderRequest);
        }
        
        // Add items to existing order
        OrderEntity existingOrder = activeOrders.get(0);
        double additionalPrice = 0.0;
        
        for (FoodOrderItemDTO foodItem : orderRequest.getFoods()) {
            Food food = foodRepository.findById(foodItem.getId())
                    .orElseThrow(() -> new RuntimeException("Food not found: " + foodItem.getId()));
            
            // Check if food already exists in order
            OrderFood existingOrderFood = existingOrder.getOrderFoods().stream()
                    .filter(of -> of.getFood().getId().equals(food.getId()))
                    .findFirst()
                    .orElse(null);
            
            if (existingOrderFood != null) {
                // Update quantity
                existingOrderFood.setQuantity(existingOrderFood.getQuantity() + foodItem.getQuantity());
                additionalPrice += food.getPrice() * foodItem.getQuantity();
            } else {
                // Add new food item
                OrderFood orderFood = new OrderFood();
                orderFood.setFood(food);
                orderFood.setQuantity(foodItem.getQuantity());
                orderFood.setOrder(existingOrder);
                
                existingOrder.getOrderFoods().add(orderFood);
                additionalPrice += food.getPrice() * foodItem.getQuantity();
            }
        }
        
        // Update total price
        existingOrder.setTotalPrice(existingOrder.getTotalPrice() + additionalPrice);
        
        return orderRepository.save(existingOrder);
    }
    
    public OrderEntity updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(status);
        return orderRepository.save(order);
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
    
    private String generateOrderNumber() {
        return "DIN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
} 