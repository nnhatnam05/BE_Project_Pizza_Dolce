package aptech.be.services.shipper;

import aptech.be.models.OrderEntity;
import aptech.be.models.UserEntity;
import aptech.be.models.shipper.ShipperProfile;
import aptech.be.repositories.OrderRepository;
import aptech.be.repositories.UserRepository;
import aptech.be.repositories.shipper.ShipperProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class OrderAssignmentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShipperProfileRepository shipperProfileRepository;

    @Transactional
    public void assignOrderToShipper(Long orderId, Long shipperId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        UserEntity shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));

        order.setShipper(shipper);
        order.setAssignedAt(LocalDateTime.now());
        order.setDeliveryStatus("WAITING_FOR_SHIPPER");

        orderRepository.save(order);
    }

    @Transactional
    public void autoAssignOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 1. Lấy danh sách shipper online và ACTIVE
        List<ShipperProfile> availableShippers = shipperProfileRepository.findAvailableShippers();

        if (availableShippers.isEmpty()) {
            throw new RuntimeException("No available shipper");
        }

        // 2. Tìm shipper có ít đơn hàng đang giao nhất
        ShipperProfile selected = availableShippers.stream()
                .min(Comparator.comparingInt(
                        sp -> orderRepository.findActiveOrdersByShipper(sp.getUser().getId()).size()
                ))
                .orElseThrow(() -> new RuntimeException("No shipper found"));

        // 3. Gán đơn hàng
        order.setShipper(selected.getUser());
        order.setAssignedAt(LocalDateTime.now());
        order.setDeliveryStatus("WAITING_FOR_SHIPPER");
        orderRepository.save(order);
    }

    @Transactional
    public void unassignOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setShipper(null);
        order.setAssignedAt(null);
        order.setDeliveryStatus("PREPARING");

        orderRepository.save(order);
    }

    public List<OrderEntity> getShipperOrders(Long shipperId, String status) {
        if (status != null) {
            return orderRepository.findByShipperIdAndStatus(shipperId, status);
        }
        return orderRepository.findByShipperIdAndDeliveryStatus(shipperId, "DELIVERING");
    }

    @Transactional
    public void acceptOrder(Long orderId, Long shipperId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        UserEntity shipper = userRepository.findById(shipperId)
                .orElseThrow(() -> new RuntimeException("Shipper not found"));
        
        order.setShipper(shipper);
        order.setDeliveryStatus("ACCEPTED");
        order.setAssignedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, Long shipperId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra shipper có quyền cập nhật đơn hàng này không
        if (!order.getShipper().getId().equals(shipperId)) {
            throw new RuntimeException("Unauthorized to update this order");
        }
        
        order.setDeliveryStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void rejectOrder(Long orderId, Long shipperId, String reason) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra shipper có quyền từ chối đơn hàng này không
        if (!order.getShipper().getId().equals(shipperId)) {
            throw new RuntimeException("Unauthorized to reject this order");
        }
        
        order.setShipper(null);
        order.setDeliveryStatus("REJECTED");
        order.setAssignedAt(null);
        orderRepository.save(order);
    }
}