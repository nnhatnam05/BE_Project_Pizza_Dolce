package aptech.be.controllers;

import aptech.be.models.Food;
import aptech.be.models.OrderEntity;
import aptech.be.repositories.FoodRepository;
import aptech.be.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:3000")
public class OrderController {

    @Autowired
    private FoodRepository  foodRepository;
    @Autowired
    private OrderRepository orderRepository;

    @GetMapping
    public List<OrderEntity> getAllOrders() {
        return orderRepository.findAll();
    }

    @PostMapping
    public OrderEntity createOrder(@RequestBody OrderEntity order) {
        order.setCreatedAt(LocalDateTime.now());

        // Load lại danh sách Food từ DB dựa theo ID
        List<Food> fullFoods = order.getFoods().stream()
                .map(f -> foodRepository.findById(f.getId())
                        .orElseThrow(() -> new RuntimeException("Food not found: " + f.getId())))
                .toList();

        order.setFoods(fullFoods);
        double total = fullFoods.stream().mapToDouble(Food::getPrice).sum();
        order.setTotalPrice(total);

        return orderRepository.save(order);
    }


    @PutMapping("/{id}")
    public OrderEntity updateOrder(@PathVariable Long id, @RequestBody OrderEntity order) {
        order.setId(id);
        return orderRepository.save(order);
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        orderRepository.deleteById(id);
    }

    @GetMapping("/filter")
    public List<OrderEntity> filterOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tableId
    ) {
        if (status != null) {
            return orderRepository.findByStatus(status);
        } else if (tableId != null) {
            return orderRepository.findByTableId(tableId);
        } else {
            return orderRepository.findAll();
        }
    }
    @GetMapping("/{id}")
    public OrderEntity getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }


}