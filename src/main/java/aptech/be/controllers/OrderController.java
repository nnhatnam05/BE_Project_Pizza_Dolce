package aptech.be.controllers;

import aptech.be.dto.*;
import aptech.be.models.*;
import aptech.be.repositories.*;
import aptech.be.services.PayOSService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
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
    private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Autowired
    private PayOSService payOSService;

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

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponseDTO createOrder(@RequestBody OrderRequestDTO orderDto, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found with email: " + email));

        Optional<OrderEntity> existingOrder = orderRepository
                .findFirstByCustomerAndConfirmStatus(customer, "WAITING_PAYMENT");
        if (existingOrder.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You already have an order waiting for confirmation. Please complete or cancel it before creating a new one."
            );
        }
        // Validate đầu vào
        if (orderDto.getFoods() == null || orderDto.getFoods().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh sách món ăn không được để trống!");
        }
        for (FoodOrderItemDTO item : orderDto.getFoods()) {
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số lượng món ăn phải lớn hơn 0!");
            }
        }
        OrderEntity order = new OrderEntity();
        order.setCreatedAt(LocalDateTime.now());

        // Gán customer đã lấy ở trên
        order.setCustomer(customer);


        // Gộp các foodId trùng, cộng dồn quantity
        Map<Long, Integer> foodIdToQuantity = orderDto.getFoods().stream()
                .collect(Collectors.toMap(
                        FoodOrderItemDTO::getId,
                        FoodOrderItemDTO::getQuantity,
                        Integer::sum // nếu trùng thì cộng số lượng
                ));

        List<OrderFood> orderFoods = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<Long, Integer> entry : foodIdToQuantity.entrySet()) {
            Long foodId = entry.getKey();
            Integer quantity = entry.getValue();
            Food food = foodRepository.findById(foodId)
                    .orElseThrow(() -> new RuntimeException("Food not found: " + foodId));
            OrderFood orderFood = new OrderFood();
            orderFood.setOrder(order);
            orderFood.setFood(food);
            orderFood.setQuantity(quantity);
            orderFoods.add(orderFood);
            total += food.getPrice() * quantity;
        }
        order.setOrderFoods(orderFoods);

        order.setNote(orderDto.getNote() != null ? orderDto.getNote() : "");
        order.setTotalPrice(total);
        if (total <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng tiền đơn hàng phải lớn hơn 0!");
        }

        Random random = new Random();
        int randomNumber = 100 + random.nextInt(900);
        order.setOrderNumber(String.valueOf(randomNumber));
        order.setStatus("WAITING_PAYMENT");
        order.setConfirmStatus("WAITING_PAYMENT");
        order.setRejectReason(null);

        // SAVE, cascade ALL sẽ tự lưu luôn orderFoods
        OrderEntity savedOrder = orderRepository.save(order);

        addOrderStatusHistory(savedOrder, "WAITING_PAYMENT", null, "system");
        return convertToDTO(savedOrder);
    }


    @PutMapping("/cancel/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponseDTO customerCancelOrder(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            Authentication authentication
    ) {
        String customerEmail = authentication.getName();
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // Kiểm tra đúng chủ đơn
        if (!order.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền hủy đơn này!");
        }

        // Kiểm tra trạng thái có thể huỷ
        if (!"WAITING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("Không thể huỷ đơn ở trạng thái hiện tại!");
        }

        // Set đầy đủ cả status và confirmStatus
        order.setStatus("REJECTED");
        order.setConfirmStatus("REJECTED");
        order.setDeliveryStatus("CANCELLED");
        order.setRejectReason(reason != null ? reason : "Cancelled by customer");
        addOrderStatusHistory(order, "CANCELLED", reason, "customer");

        orderRepository.save(order);
        return convertToDTO(order);
    }




    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponseDTO updateOrder(
            @PathVariable Long id,
            @RequestBody OrderRequestDTO orderDto,
            Authentication authentication) {

        String customerEmail = authentication.getName();

        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // Kiểm tra đúng chủ đơn
        if (!order.getCustomer().getEmail().equalsIgnoreCase(customerEmail)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa đơn này!");
        }

        // Chỉ cho phép sửa khi đơn đang chờ xác nhận
        if (!"WAITING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("Chỉ được chỉnh sửa đơn khi đang chờ xác nhận!");
        }

        // Bắt buộc phải có món ăn
        if (orderDto.getFoods() == null || orderDto.getFoods().isEmpty()) {
            throw new RuntimeException("Đơn hàng phải có ít nhất một món ăn!");
        }


        // Gộp lại các món trùng id
        Map<Long, Integer> foodIdToQuantity = orderDto.getFoods().stream()
                .collect(Collectors.toMap(
                        FoodOrderItemDTO::getId,
                        FoodOrderItemDTO::getQuantity,
                        Integer::sum
                ));

        order.getOrderFoods().clear();
        orderRepository.saveAndFlush(order);
        List<OrderFood> orderFoods = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<Long, Integer> entry : foodIdToQuantity.entrySet()) {
            Long foodId = entry.getKey();
            Integer quantity = entry.getValue();
            Food food = foodRepository.findById(foodId)
                    .orElseThrow(() -> new RuntimeException("Food not found: " + foodId));
            OrderFood orderFood = new OrderFood();
            orderFood.setOrder(order);
            orderFood.setFood(food);
            orderFood.setQuantity(quantity);

            // Phải set lại composite id nếu dùng EmbeddedId (rất quan trọng)
            OrderFoodId ofId = new OrderFoodId();
            ofId.setOrderId(order.getId());
            ofId.setFoodId(food.getId());
            orderFood.setId(ofId);

            orderFoods.add(orderFood);
            total += food.getPrice() * quantity;
        }


        order.getOrderFoods().addAll(orderFoods);

        order.setTotalPrice(total);

        // Sửa ghi chú (nếu truyền lên)
        if (orderDto.getNote() != null) {
            order.setNote(orderDto.getNote());
        }

        OrderEntity savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }



    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public void deleteOrder(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        // Nếu là admin thì cho phép xóa luôn, còn customer thì kiểm tra chủ đơn
        if (order.getCustomer() != null &&
                !order.getCustomer().getEmail().equalsIgnoreCase(email) &&
                !authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Bạn không có quyền xóa đơn này!");
        }
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
        dto.setNote(order.getNote());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setConfirmStatus(order.getConfirmStatus());
        dto.setRejectReason(order.getRejectReason());
        dto.setDeliveryStatus(order.getDeliveryStatus());
        dto.setDeliveryNote(order.getDeliveryNote());

        // Chỉnh lại: foods kèm quantity
        dto.setFoodList(order.getOrderFoods().stream().map(orderFood -> {
            Food food = orderFood.getFood();
            FoodDTO foodDto = new FoodDTO();
            foodDto.setId(food.getId());
            foodDto.setName(food.getName());
            foodDto.setPrice(food.getPrice());
            foodDto.setQuantity(orderFood.getQuantity()); // quantity lấy từ orderFood
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

//    @GetMapping("/waiting-confirm")
//    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
//    public List<OrderResponseDTO> getOrdersWaitingConfirm(Authentication authentication) {
//        System.out.println("Authorities: " + authentication.getAuthorities());
//        return orderRepository.findByConfirmStatus("WAITING_PAYMENT")
//                .stream().map(this::convertToDTO).collect(Collectors.toList());
//    }


//    @PutMapping("/{id}/admin-confirm")
//    @PreAuthorize("hasRole('ADMIN')")
//    public OrderResponseDTO adminConfirmOrder(
//            @PathVariable Long id,
//            @RequestParam("action") String action,
//            @RequestParam(value = "reason", required = false) String reason
//    ) {
//        OrderEntity order = orderRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
//
//        if (!"WAITING_PAYMENT".equals(order.getConfirmStatus())) {
//            throw new RuntimeException("Order is not waiting for confirmation!");
//        }
//
//        if ("confirm".equalsIgnoreCase(action)) {
//            order.setConfirmStatus("CONFIRMED");
//            order.setStatus("CONFIRMED");
//            order.setRejectReason(null);
//            order.setDeliveryStatus("PREPARING"); // <-- Thêm dòng này
//            addOrderStatusHistory(order, "CONFIRMED", null, "admin");
//            addOrderStatusHistory(order, "PREPARING", null, "system"); // Ghi nhận mốc này vào lịch sử
//        } else if ("reject".equalsIgnoreCase(action)) {
//            order.setConfirmStatus("REJECTED");
//            order.setStatus("REJECTED");
//            order.setRejectReason(reason != null ? reason : "No reason provided");
//            addOrderStatusHistory(order, "REJECTED", reason, "admin");
//        } else {
//            throw new RuntimeException("Invalid action");
//        }
//        orderRepository.save(order);
//        return convertToDTO(order);
//    }

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

        // Kiểm tra đơn đã thanh toán và có thể cập nhật trạng thái giao hàng
        String currentStatus = order.getStatus();
        if (!"PAID".equals(currentStatus) && 
            !"PREPARING".equals(currentStatus) && 
            !"WAITING_FOR_SHIPPER".equals(currentStatus) && 
            !"DELIVERING".equals(currentStatus)) {
            throw new RuntimeException("Order must be paid before delivery!");
        }

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

    @GetMapping("/myorder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponseDTO> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Not found!"));
        // Trả về tất cả orders của customer
        List<OrderEntity> orders = orderRepository.findByCustomerId(customer.getId());
        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Endpoint này đã được gộp vào /myorder

    @GetMapping("/my/{id}/delivery-status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderDeliveryStatusDTO getMyOrderDeliveryStatus(@PathVariable Long id, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Not found!"));

        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        // Chỉ trả về nếu đúng chủ đơn
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You do not have permission to view this order!");
        }

        // Lấy lịch sử trạng thái giao hàng
        List<OrderStatusHistoryDTO> history = orderStatusHistoryRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId())
                .stream()
                .map(h -> {
                    OrderStatusHistoryDTO s = new OrderStatusHistoryDTO();
                    s.setStatus(h.getStatus());
                    s.setNote(h.getNote());
                    s.setChangedAt(h.getChangedAt());
                    s.setChangedBy(h.getChangedBy());
                    return s;
                }).collect(Collectors.toList());

        // Trả về trạng thái hiện tại và lịch sử
        OrderDeliveryStatusDTO dto = new OrderDeliveryStatusDTO();
        dto.setOrderId(order.getId());
        
        // Nếu deliveryStatus rỗng nhưng có history, lấy trạng thái cuối cùng
        String currentDeliveryStatus = order.getDeliveryStatus();
        if ((currentDeliveryStatus == null || currentDeliveryStatus.isEmpty()) && !history.isEmpty()) {
            currentDeliveryStatus = history.get(history.size() - 1).getStatus();
        }
        
        dto.setDeliveryStatus(currentDeliveryStatus);
        dto.setDeliveryNote(order.getDeliveryNote());
        dto.setStatusHistory(history);

        return dto;
    }

    /**
     * Tạo URL thanh toán PayOS cho đơn hàng
     */
    @GetMapping("/payment/qr/{orderId}")
    public ResponseEntity<String> getPayOSPaymentUrl(@PathVariable Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Lấy thông tin customer từ order
        Customer customer = order.getCustomer();

        // Lấy thông tin customer detail
        String buyerName = customer.getFullName();
        String buyerEmail = customer.getEmail();
        String buyerPhone = "";
        String buyerAddress = "";

        if (customer.getCustomerDetail() != null) {
            buyerPhone = customer.getCustomerDetail().getPhoneNumber() != null ? customer.getCustomerDetail().getPhoneNumber() : "";
            buyerAddress = customer.getCustomerDetail().getAddress() != null ? customer.getCustomerDetail().getAddress() : "";
        }

        Double totalPriceUSD = order.getTotalPrice();
        if (totalPriceUSD == null || totalPriceUSD <= 0) {
            throw new RuntimeException("Total price không hợp lệ: " + totalPriceUSD);
            }
        
        // Chuyển đổi từ USD sang VND (1 USD = 24,000 VND)
        double exchangeRate = 24000.0;
        double totalPriceVND = totalPriceUSD * exchangeRate;
        int amountInVND = (int) Math.round(totalPriceVND);
        
        // Giới hạn PayOS: 1,000 - 1,000,000 VND
        if (amountInVND < 1000) amountInVND = 1000;
        if (amountInVND > 1000000) amountInVND = 1000000;

        String orderInfo = "Thanh toán đơn hàng #" + order.getOrderNumber();

        // Gọi service mới, truyền thông tin buyer
        String paymentUrl = payOSService.createPaymentUrlWithBuyer(
            order.getId(),
            amountInVND,
            orderInfo,
            buyerName,
            buyerEmail,
            buyerPhone,
            buyerAddress
        );

        return ResponseEntity.ok(paymentUrl);
            }

    /**
     * Nhận webhook từ PayOS (server-to-server)
     */
    @PostMapping("/payment/payos/webhook")
    public ResponseEntity<Map<String, String>> handlePayOSWebhook(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> jsonData) {
        Map<String, Object> webhookData = new HashMap<>();
        
        // Xử lý cả JSON và form data
        if (jsonData != null && !jsonData.isEmpty()) {
            webhookData.putAll(jsonData);
            System.out.println("[PAYOS WEBHOOK] Received JSON webhook data: " + webhookData);
        } else {
            request.getParameterMap().forEach((k, v) -> webhookData.put(k, v[0]));
            System.out.println("[PAYOS WEBHOOK] Received form webhook data: " + webhookData);
        }
        
        String signature = request.getHeader("x-signature");
        System.out.println("[PAYOS WEBHOOK] Signature: " + signature);
        
        boolean valid = payOSService.verifyWebhook(webhookData, signature);
        Map<String, String> response = new HashMap<>();
        
        if (!valid) {
            System.out.println("[PAYOS WEBHOOK] Invalid signature");
            response.put("code", "97");
            response.put("message", "Invalid Signature");
            return ResponseEntity.ok(response);
        }
        
        try {
            String orderCode = (String) webhookData.get("orderCode");
            String status = (String) webhookData.get("status");
            
            // orderCode là timestamp, tìm order theo thời gian tạo gần nhất
            Long orderCodeLong = Long.valueOf(orderCode);
            
            // Tìm order có status WAITING_PAYMENT và thời gian tạo gần với orderCode
            List<OrderEntity> waitingOrders = orderRepository.findByStatus("WAITING_PAYMENT");
            System.out.println("[PAYOS WEBHOOK] Found " + waitingOrders.size() + " waiting orders");
            
            OrderEntity order = null;
            
            for (OrderEntity waitingOrder : waitingOrders) {
                if (waitingOrder.getCreatedAt() != null) {
                    long orderTime = waitingOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    System.out.println("[PAYOS WEBHOOK] Order " + waitingOrder.getId() + " created at: " + orderTime + ", orderCode: " + orderCodeLong);
                    // Kiểm tra nếu orderCode gần với thời gian tạo order (trong khoảng 5 phút)
                    if (Math.abs(orderTime - orderCodeLong) < 300000) { // 5 phút = 300,000 ms
                        order = waitingOrder;
                        System.out.println("[PAYOS WEBHOOK] Found matching order: " + order.getId());
                        break;
                    }
                }
            }
            
            if (order == null) {
                System.out.println("[PAYOS WEBHOOK] No matching order found for orderCode: " + orderCode);
                throw new RuntimeException("Order not found with orderCode: " + orderCode);
            }
            
            if ("PAID".equals(order.getStatus())) {
                response.put("code", "02");
                response.put("message", "Order already confirmed");
                return ResponseEntity.ok(response);
            }
            
            System.out.println("[PAYOS WEBHOOK] Processing payment status: " + status + " for order: " + order.getId());
            
            if ("PAID".equals(status)) {
                System.out.println("[PAYOS WEBHOOK] Payment successful, updating order status to PAID");
            order.setStatus("PAID");
            order.setConfirmStatus("PAID");
                order.setDeliveryStatus("PREPARING");
            orderRepository.save(order);
            addOrderStatusHistory(order, "PAID", "Thanh toán thành công qua PayOS", "system");
                System.out.println("[PAYOS WEBHOOK] Order " + order.getId() + " status updated to PAID");
            } else {
                System.out.println("[PAYOS WEBHOOK] Payment failed, updating order status to FAILED");
                order.setStatus("CANCELLED");
                order.setConfirmStatus("CANCELLED");
                order.setDeliveryStatus("CANCELLED");
                orderRepository.save(order);
                addOrderStatusHistory(order, "FAILED", "Thanh toán thất bại qua PayOS", "system");
                System.out.println("[PAYOS WEBHOOK] Order " + order.getId() + " status updated to FAILED");
            }
            
            response.put("code", "00");
            response.put("message", "Confirm Success");
            System.out.println("[PAYOS WEBHOOK] Webhook processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("code", "99");
            response.put("message", "Unknown error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Nhận redirect từ PayOS về (ReturnUrl)
     */
    @GetMapping("/payment/payos/return")
    public ResponseEntity<String> handlePayOSReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
        
        System.out.println("[PAYOS RETURN] Received return params: " + params);
        
        String status = params.get("status");
        String orderCode = params.get("orderCode");
        
        // Tự động cập nhật status nếu thanh toán thành công
        if ("PAID".equals(status) && orderCode != null) {
            try {
                System.out.println("[PAYOS RETURN] Processing successful payment for orderCode: " + orderCode);
                
                Long orderCodeLong = Long.valueOf(orderCode);
                List<OrderEntity> waitingOrders = orderRepository.findByStatus("WAITING_PAYMENT");
                
                OrderEntity order = null;
                for (OrderEntity waitingOrder : waitingOrders) {
                    if (waitingOrder.getCreatedAt() != null) {
                        long orderTime = waitingOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                        if (Math.abs(orderTime - orderCodeLong) < 300000) { // 5 phút
                            order = waitingOrder;
                            break;
                        }
                    }
                }
                
                if (order != null) {
                    System.out.println("[PAYOS RETURN] Found order: " + order.getId() + ", updating status to PAID");
                    order.setStatus("PAID");
                    order.setConfirmStatus("PAID");
                    order.setDeliveryStatus("PREPARING");
                    orderRepository.save(order);
                    addOrderStatusHistory(order, "PAID", "Thanh toán thành công qua PayOS Return URL", "system");
                    System.out.println("[PAYOS RETURN] Order status updated successfully");
                } else {
                    System.out.println("[PAYOS RETURN] No matching order found for orderCode: " + orderCode);
                }
            } catch (Exception e) {
                System.err.println("[PAYOS RETURN] Error updating order status: " + e.getMessage());
            }
        }
        
        // Redirect về frontend với thông tin thanh toán
        String frontendUrl = "http://localhost:3000/payment-success";
        if ("PAID".equals(status)) {
            frontendUrl += "?status=success&orderCode=" + orderCode;
        } else {
            frontendUrl += "?status=failed&orderCode=" + orderCode;
        }
        
        // Return HTML redirect
        String html = "<!DOCTYPE html><html><head><title>Redirecting...</title></head><body>" +
                     "<script>window.location.href='" + frontendUrl + "';</script>" +
                     "<p>Redirecting to payment result page...</p></body></html>";
        
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .header("ngrok-skip-browser-warning", "true")
                .body(html);
    }

    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    public void cancelExpiredOrders() {
        LocalDateTime twentyMinutesAgo = LocalDateTime.now().minusMinutes(20);

        List<OrderEntity> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                "WAITING_PAYMENT",
                twentyMinutesAgo
        );

        for (OrderEntity order : expiredOrders) {
            // Cập nhật trạng thái đơn hàng
            order.setStatus("CANCELLED");
            order.setConfirmStatus("CANCELLED");
            order.setDeliveryStatus("CANCELLED");
            order.setRejectReason("Tự động hủy do không thanh toán sau 20 phút");

            // Lưu đơn hàng
            orderRepository.save(order);

            // Ghi lịch sử trạng thái
            addOrderStatusHistory(order, "CANCELLED",
                    "Tự động hủy do không thanh toán sau 20 phút", "system");

            System.out.println("Đã hủy đơn hàng #" + order.getOrderNumber() + " do quá hạn thanh toán");
        }
    }

    @GetMapping("/payment/status/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> getPaymentStatus(@PathVariable Long orderId, Authentication authentication) {
        String email = authentication.getName();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra quyền xem đơn hàng
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You do not have permission to view this order!");
        }

        Map<String, String> response = new HashMap<>();
        response.put("orderId", orderId.toString());
        response.put("status", order.getStatus());
        response.put("orderNumber", order.getOrderNumber());

        return ResponseEntity.ok(response);
    }

    /**
     * Tìm order theo orderCode (timestamp)
     */
    @GetMapping("/payment/find-by-ordercode")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, Object>> findOrderByOrderCode(
            @RequestParam String orderCode, 
            Authentication authentication) {
        try {
            String customerEmail = authentication.getName();
            Long orderCodeLong = Long.valueOf(orderCode);
            
            // Tìm order có status PAID và thời gian tạo gần với orderCode
            List<OrderEntity> paidOrders = orderRepository.findByStatus("PAID");
            OrderEntity order = null;
            
            for (OrderEntity paidOrder : paidOrders) {
                if (paidOrder.getCustomer().getEmail().equalsIgnoreCase(customerEmail) && 
                    paidOrder.getCreatedAt() != null) {
                    long orderTime = paidOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    // Kiểm tra nếu orderCode gần với thời gian tạo order (trong khoảng 5 phút)
                    if (Math.abs(orderTime - orderCodeLong) < 300000) { // 5 phút = 300,000 ms
                        order = paidOrder;
                        break;
                    }
                }
            }
            
            if (order == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.getId());
            response.put("orderNumber", order.getOrderNumber());
            response.put("status", order.getStatus());
            response.put("totalPrice", order.getTotalPrice());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Endpoint test PayOS (dùng cho Postman)
     */
    @PostMapping("/payos/test-create-url")
    public ResponseEntity<String> testPayOSCreateUrl(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.getOrDefault("orderId", 1).toString());
        double amountUSD = Double.parseDouble(body.getOrDefault("amount", 10.0).toString());
        
        // Chuyển đổi từ USD sang VND
        double exchangeRate = 24000.0;
        int amountVND = (int) Math.round(amountUSD * exchangeRate);
        
        String orderInfo = body.getOrDefault("orderInfo", "Test order").toString();
        String url = payOSService.createPaymentUrl(orderId, amountVND, orderInfo);
        return ResponseEntity.ok(url);
    }

    /**
     * Test endpoint để kiểm tra backend có hoạt động không
     */
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Backend is running! Current time: " + java.time.LocalDateTime.now());
    }

    /**
     * Test webhook endpoint
     */
    @PostMapping("/test-webhook")
    public ResponseEntity<Map<String, String>> testWebhook(@RequestBody Map<String, Object> webhookData) {
        System.out.println("[TEST WEBHOOK] Received test webhook data: " + webhookData);
        
        Map<String, String> response = new HashMap<>();
        response.put("code", "00");
        response.put("message", "Test webhook received successfully");
        
        return ResponseEntity.ok(response);
    }

        /**
     * Manual update order status (for testing)
     */
    @PostMapping("/manual-update-status")
    public ResponseEntity<Map<String, String>> manualUpdateStatus(@RequestBody Map<String, Object> request) {
        try {
            String orderCode = (String) request.get("orderCode");
            String status = (String) request.get("status");
            
            System.out.println("[MANUAL UPDATE] Updating order with orderCode: " + orderCode + ", status: " + status);
            
            Long orderCodeLong = Long.valueOf(orderCode);
            List<OrderEntity> waitingOrders = orderRepository.findByStatus("WAITING_PAYMENT");
            
            OrderEntity order = null;
            for (OrderEntity waitingOrder : waitingOrders) {
                if (waitingOrder.getCreatedAt() != null) {
                    long orderTime = waitingOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (Math.abs(orderTime - orderCodeLong) < 300000) {
                        order = waitingOrder;
                        break;
                    }
                }
            }
            
            if (order == null) {
                throw new RuntimeException("Order not found with orderCode: " + orderCode);
            }
            
            if ("PAID".equals(status)) {
                order.setStatus("PAID");
                order.setConfirmStatus("PAID");
                order.setDeliveryStatus("PREPARING");
                orderRepository.save(order);
                addOrderStatusHistory(order, "PAID", "Manual update - Thanh toán thành công", "system");
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("code", "00");
            response.put("message", "Order updated successfully");
            response.put("orderId", order.getId().toString());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("code", "99");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Force update order status by orderCode (GET method for easy testing)
     */
    @GetMapping("/force-update-status")
    public ResponseEntity<Map<String, String>> forceUpdateStatus(
            @RequestParam String orderCode,
            @RequestParam(defaultValue = "PAID") String status) {
        try {
            System.out.println("[FORCE UPDATE] Updating order with orderCode: " + orderCode + ", status: " + status);
            
            Long orderCodeLong = Long.valueOf(orderCode);
            List<OrderEntity> waitingOrders = orderRepository.findByStatus("WAITING_PAYMENT");
            
            OrderEntity order = null;
            for (OrderEntity waitingOrder : waitingOrders) {
                if (waitingOrder.getCreatedAt() != null) {
                    long orderTime = waitingOrder.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (Math.abs(orderTime - orderCodeLong) < 300000) {
                        order = waitingOrder;
                        break;
                    }
                }
            }
            
            if (order == null) {
                throw new RuntimeException("Order not found with orderCode: " + orderCode);
            }
            
            if ("PAID".equals(status)) {
            order.setStatus("PAID");
            order.setConfirmStatus("PAID");
            order.setDeliveryStatus("PREPARING");
            orderRepository.save(order);
                addOrderStatusHistory(order, "PAID", "Force update - Thanh toán thành công", "system");
                System.out.println("[FORCE UPDATE] Order " + order.getId() + " updated to PAID");
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("code", "00");
            response.put("message", "Order updated successfully");
            response.put("orderId", order.getId().toString());
            response.put("orderNumber", order.getOrderNumber());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("code", "99");
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }


}
