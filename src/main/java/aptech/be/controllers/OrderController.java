package aptech.be.controllers;

import aptech.be.dto.*;
import aptech.be.models.*;
import aptech.be.repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    @Autowired
    private FoodRepository foodRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PaymentMethodRepository paymentMethodRepository;
    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @GetMapping
    public List<OrderResponseDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public OrderResponseDTO getOrderById(@PathVariable Long id) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDTO(order);
    }

    @PostMapping
    public OrderResponseDTO createOrder(@RequestBody OrderRequestDTO orderDto) {
        OrderEntity order = new OrderEntity();
        order.setCreatedAt(LocalDateTime.now());

        // Gán customer
        Customer customer = customerRepository.findById(orderDto.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found: " + orderDto.getCustomerId()));
        order.setCustomer(customer);

        // Gán phương thức thanh toán
        PaymentMethod paymentMethod = paymentMethodRepository.findById(orderDto.getPaymentMethodId())
                .orElseThrow(() -> new RuntimeException("Payment method not found: " + orderDto.getPaymentMethodId()));
        order.setPaymentMethod(paymentMethod);

        // Load danh sách Food
        List<Food> foods = orderDto.getFoodIds().stream()
                .map(id -> foodRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Food not found: " + id)))
                .collect(Collectors.toList());
        order.setFoods(foods);

        double total = foods.stream().mapToDouble(Food::getPrice).sum();
        order.setTotalPrice(total);
        Random random = new Random();
        int randomNumber = 100 + random.nextInt(900);
        order.setOrderNumber(String.valueOf(randomNumber));
        order.setStatus("WAITING_CONFIRM");

        order.setConfirmStatus("WAITING_CONFIRM");
        order.setRejectReason(null);

        OrderEntity savedOrder = orderRepository.save(order);

        // Thêm lịch sử trạng thái
        addOrderStatusHistory(savedOrder, "WAITING_CONFIRM", null, "system");

        return convertToDTO(savedOrder);
    }

    @PutMapping("/{id}")
    public OrderResponseDTO updateOrder(@PathVariable Long id, @RequestBody OrderRequestDTO orderDto) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        List<Food> foods = orderDto.getFoodIds().stream()
                .map(foodId -> foodRepository.findById(foodId)
                        .orElseThrow(() -> new RuntimeException("Food not found: " + foodId)))
                .collect(Collectors.toList());
        order.setFoods(foods);

        double total = foods.stream().mapToDouble(Food::getPrice).sum();
        order.setTotalPrice(total);

        if (orderDto.getPaymentMethodId() != null) {
            PaymentMethod paymentMethod = paymentMethodRepository.findById(orderDto.getPaymentMethodId())
                    .orElseThrow(() -> new RuntimeException("Payment method not found: " + orderDto.getPaymentMethodId()));
            order.setPaymentMethod(paymentMethod);
        }

        OrderEntity savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void deleteOrder(@PathVariable Long id) {
        orderStatusHistoryRepository.deleteByOrderId(id);
        orderRepository.deleteById(id);
    }

    @GetMapping("/filter")
    public List<OrderResponseDTO> filterOrders(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) Long customerId) {
        List<OrderEntity> orders;
        if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId);
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private OrderResponseDTO convertToDTO(OrderEntity order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTotalPrice(order.getTotalPrice());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setConfirmStatus(order.getConfirmStatus());
        dto.setRejectReason(order.getRejectReason());
        dto.setDeliveryStatus(order.getDeliveryStatus());
        dto.setDeliveryNote(order.getDeliveryNote());

        // foods
        dto.setFoodList(order.getFoods().stream().map(food -> {
            FoodDTO foodDto = new FoodDTO();
            foodDto.setId(food.getId());
            foodDto.setName(food.getName());
            foodDto.setPrice(food.getPrice());
            return foodDto;
        }).collect(Collectors.toList()));

        // customer
        Customer customer = order.getCustomer();
        if (customer != null) {
            CustomerDTO customerDTO = new CustomerDTO();
            customerDTO.setId(customer.getId());
            customerDTO.setFullName(customer.getFullName());
            customerDTO.setEmail(customer.getEmail());
            if (customer.getCustomerDetail() != null) {
                customerDTO.setPhoneNumber(customer.getCustomerDetail().getPhoneNumber());
                customerDTO.setAddress(customer.getCustomerDetail().getAddress());
                customerDTO.setImageUrl(customer.getCustomerDetail().getImageUrl());
                customerDTO.setPoint(customer.getCustomerDetail().getPoint());
                customerDTO.setVoucher(customer.getCustomerDetail().getVoucher());
            }
            dto.setCustomer(customerDTO);
        }

        // payment method
        PaymentMethod paymentMethod = order.getPaymentMethod();
        if (paymentMethod != null) {
            PaymentMethodDTO pmDto = new PaymentMethodDTO();
            pmDto.setId(paymentMethod.getId());
            pmDto.setName(paymentMethod.getName());
            dto.setPaymentMethod(pmDto);
        }

        // Lịch sử trạng thái
        List<OrderStatusHistory> historyList = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(order.getId());
        List<OrderStatusHistoryDTO> historyDTOs = historyList.stream().map(history -> {
            OrderStatusHistoryDTO statusDto = new OrderStatusHistoryDTO();
            statusDto.setStatus(history.getStatus());
            statusDto.setNote(history.getNote());
            statusDto.setChangedAt(history.getChangedAt());
            statusDto.setChangedBy(history.getChangedBy());
            return statusDto;
        }).collect(Collectors.toList());
        dto.setStatusHistory(historyDTOs);

        return dto;
    }

    @GetMapping("/{id}/status-history")
    public List<OrderStatusHistoryDTO> getOrderStatusHistory(@PathVariable Long id) {
        List<OrderStatusHistory> historyList = orderStatusHistoryRepository.findByOrderIdOrderByChangedAtAsc(id);
        return historyList.stream().map(history -> {
            OrderStatusHistoryDTO dto = new OrderStatusHistoryDTO();
            dto.setStatus(history.getStatus());
            dto.setNote(history.getNote());
            dto.setChangedAt(history.getChangedAt());
            dto.setChangedBy(history.getChangedBy());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/waiting-confirm")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public List<OrderResponseDTO> getOrdersWaitingConfirm() {
        List<OrderEntity> orders = orderRepository.findByConfirmStatus("WAITING_CONFIRM");
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @PutMapping("/{id}/admin-confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderResponseDTO adminConfirmOrder(
            @PathVariable Long id,
            @RequestParam("action") String action,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (!"WAITING_CONFIRM".equals(order.getConfirmStatus())) {
            throw new RuntimeException("Order is not waiting for confirmation!");
        }

        if ("confirm".equalsIgnoreCase(action)) {
            order.setConfirmStatus("CONFIRMED");
            order.setStatus("CONFIRMED");
            order.setRejectReason(null);
            order.setDeliveryStatus("PREPARING"); // <-- Thêm dòng này
            addOrderStatusHistory(order, "CONFIRMED", null, "admin");
            addOrderStatusHistory(order, "PREPARING", null, "system"); // Ghi nhận mốc này vào lịch sử
        } else if ("reject".equalsIgnoreCase(action)) {
            order.setConfirmStatus("REJECTED");
            order.setStatus("REJECTED");
            order.setRejectReason(reason != null ? reason : "No reason provided");
            addOrderStatusHistory(order, "REJECTED", reason, "admin");
        } else {
            throw new RuntimeException("Invalid action");
        }
        orderRepository.save(order);
        return convertToDTO(order);
    }

    @PutMapping("/{id}/delivery-status")
    @PreAuthorize("hasRole('STAFF')")
    public OrderResponseDTO updateDeliveryStatus(
            @PathVariable Long id,
            @RequestParam("status") String status,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "cancelReason", required = false) String cancelReason
    ) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        switch (status.toUpperCase()) {
            case "PREPARING":
                order.setDeliveryStatus("PREPARING");
                order.setStatus("PREPARING");
                order.setDeliveryNote(note);
                addOrderStatusHistory(order, "PREPARING", note, "staff");
                break;
            case "WAITING_FOR_SHIPPER":
                order.setDeliveryStatus("WAITING_FOR_SHIPPER");
                order.setStatus("WAITING_FOR_SHIPPER");
                order.setDeliveryNote(note);
                addOrderStatusHistory(order, "WAITING_FOR_SHIPPER", note, "staff");
                break;
            case "DELIVERING":
                order.setDeliveryStatus("DELIVERING");
                order.setStatus("DELIVERING");
                order.setDeliveryNote(note);
                addOrderStatusHistory(order, "DELIVERING", note, "staff");
                break;
            case "DELIVERED":
                order.setDeliveryStatus("DELIVERED");
                order.setStatus("DELIVERED");
                order.setDeliveryNote(note);
                addOrderStatusHistory(order, "DELIVERED", note, "staff");
                break;
            case "CANCELLED":
                order.setDeliveryStatus("CANCELLED");
                order.setStatus("CANCELLED");
                order.setDeliveryNote(note);
                order.setRejectReason(cancelReason != null ? cancelReason : "Cancelled by staff");
                addOrderStatusHistory(order, "CANCELLED", cancelReason != null ? cancelReason : note, "staff");
                break;
            default:
                throw new RuntimeException("Invalid delivery status");
        }

        orderRepository.save(order);
        return convertToDTO(order);
    }

    // Hàm thêm lịch sử trạng thái đơn hàng
    private void addOrderStatusHistory(OrderEntity order, String status, String note, String changedBy) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setStatus(status);
        history.setNote(note);
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy(changedBy);
        orderStatusHistoryRepository.save(history);
    }
}
