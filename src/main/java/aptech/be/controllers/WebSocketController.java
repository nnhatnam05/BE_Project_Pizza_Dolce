package aptech.be.controllers;

import aptech.be.dto.NotificationDTO;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class WebSocketController {

    /**
     * Handle subscription to staff notifications
     */
    @SubscribeMapping("/topic/staff/notifications")
    public NotificationDTO handleStaffNotificationSubscription() {
        return new NotificationDTO(
            "SYSTEM",
            "Connected",
            "You are now connected to real-time notifications",
            null,
            LocalDateTime.now(),
            "LOW"
        );
    }

    /**
     * Handle subscription to order updates
     */
    @SubscribeMapping("/topic/staff/orders")
    public String handleOrderSubscription() {
        return "Connected to order updates";
    }

    /**
     * Handle subscription to staff calls
     */
    @SubscribeMapping("/topic/staff/calls")
    public String handleStaffCallSubscription() {
        return "Connected to staff call notifications";
    }

    /**
     * Handle subscription to payment requests
     */
    @SubscribeMapping("/topic/staff/payments")
    public String handlePaymentSubscription() {
        return "Connected to payment request notifications";
    }

    /**
     * Handle ping messages to keep connection alive
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public String handlePing() {
        return "pong";
    }

    // Clients subscribe to: /topic/complaints/{id}
} 