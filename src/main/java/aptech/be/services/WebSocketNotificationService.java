package aptech.be.services;

import aptech.be.dto.AccountDeactivationDTO;
import aptech.be.dto.NotificationDTO;
import aptech.be.dto.OrderNotificationDTO;
import aptech.be.models.OrderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebSocketNotificationService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send notification to all staff members
     */
    public void sendToStaff(NotificationDTO notification) {
        messagingTemplate.convertAndSend("/topic/staff/notifications", notification);
    }
    
    /**
     * Send notification to specific table
     */
    public void sendToTable(Long tableId, NotificationDTO notification) {
        messagingTemplate.convertAndSend("/topic/table/" + tableId, notification);
    }
    
    /**
     * Send order update to staff dashboard
     */
    public void sendOrderUpdate(Object orderData) {
        messagingTemplate.convertAndSend("/topic/staff/orders", orderData);
    }
    
    /**
     * Send table status update
     */
    public void sendTableStatusUpdate(Object tableData) {
        messagingTemplate.convertAndSend("/topic/staff/tables", tableData);
    }
    
    /**
     * Send account deactivation notification to specific user
     * This will trigger logout and redirect to login page
     */
    public void sendAccountDeactivationNotification(String userId, String username, String userType) {
        try {
            AccountDeactivationDTO deactivationNotification = AccountDeactivationDTO.accountDeactivated(userId, username, userType);
            
            // Send to specific user topic
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/account-status", deactivationNotification);
            
            // Also send to general account status topic for broader listening
            messagingTemplate.convertAndSend("/topic/account-status", deactivationNotification);
            
            System.out.println("[WEBSOCKET] Account deactivation notification sent to user: " + username + " (ID: " + userId + ", Type: " + userType + ")");
        } catch (Exception e) {
            System.err.println("[WEBSOCKET ERROR] Failed to send account deactivation notification: " + e.getMessage());
        }
    }
    
    /**
     * Send account activation notification to specific user
     */
    public void sendAccountActivationNotification(String userId, String username, String userType) {
        try {
            AccountDeactivationDTO activationNotification = new AccountDeactivationDTO(
                "ACCOUNT_ACTIVATED",
                "Your account has been activated by administrator",
                "Account activated by admin",
                userId,
                username,
                userType
            );
            
            // Send to specific user topic
            messagingTemplate.convertAndSend("/topic/user/" + userId + "/account-status", activationNotification);
            
            // Also send to general account status topic
            messagingTemplate.convertAndSend("/topic/account-status", activationNotification);
            
            System.out.println("[WEBSOCKET] Account activation notification sent to user: " + username + " (ID: " + userId + ", Type: " + userType + ")");
        } catch (Exception e) {
            System.err.println("[WEBSOCKET ERROR] Failed to send account activation notification: " + e.getMessage());
        }
    }
    
    /**
     * Send staff call notification (with table number)
     */
    public void sendStaffCallNotification(Long tableId, String reason, int tableNumber) {
        NotificationDTO notification = NotificationDTO.staffCall(tableId, reason);
        
        // Add table number to notification data
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("tableId", tableId);
        notificationData.put("tableNumber", tableNumber);
        notificationData.put("reason", reason);
        notificationData.put("callTime", LocalDateTime.now());
        
        // Create enhanced notification with table number
        NotificationDTO enhancedNotification = new NotificationDTO(
            "STAFF_CALL",
            "Staff Call Request", 
            "Table " + tableNumber + " is requesting assistance: " + reason,
            notificationData,
            LocalDateTime.now(),
            "HIGH"
        );
        
        // Send to staff calls topic for real-time updates (only one send)
        messagingTemplate.convertAndSend("/topic/staff/calls", enhancedNotification);
        
        System.out.println("Staff call notification sent for table " + tableId + " (Table " + tableNumber + "): " + reason);
    }
    
    /**
     * Send staff call notification (backward compatibility)
     */
    public void sendStaffCallNotification(Long tableId, String reason) {
        // Default to unknown table number for backward compatibility
        sendStaffCallNotification(tableId, reason, 0);
    }
    
    /**
     * Send payment request notification (with table number)
     */
    public void sendPaymentRequestNotification(Long tableId, int tableNumber) {
        NotificationDTO notification = NotificationDTO.paymentRequest(tableId);
        
        // Add table number to notification data
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("tableId", tableId);
        notificationData.put("tableNumber", tableNumber);
        notificationData.put("requestTime", LocalDateTime.now());
        
        // Create enhanced notification with table number
        NotificationDTO enhancedNotification = new NotificationDTO(
            "PAYMENT_REQUEST",
            "Payment Request",
            "Table " + tableNumber + " is requesting payment",
            notificationData,
            LocalDateTime.now(),
            "HIGH"
        );
        
        // Send to payment requests topic for real-time updates (only one send)
        messagingTemplate.convertAndSend("/topic/staff/payments", enhancedNotification);
        
        System.out.println("Payment request notification sent for table " + tableId + " (Table " + tableNumber + ")");
    }
    
    /**
     * Send payment request notification (backward compatibility)
     */
    public void sendPaymentRequestNotification(Long tableId) {
        // Default to unknown table number for backward compatibility
        sendPaymentRequestNotification(tableId, 0);
    }
    
    /**
     * Send new order notification
     */
    public void sendNewOrderNotification(Object orderData) {
        try {
            if (orderData instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) orderData;
                
                // Create simple notification without complex data
                NotificationDTO notification = new NotificationDTO(
                    "NEW_ORDER",
                    "New Dine-In Order",
                    "A new dine-in order has been placed at Table " + 
                    (order.getTable() != null ? order.getTable().getNumber() : "Unknown"),
                    null, // No complex data
                    java.time.LocalDateTime.now(),
                    "HIGH"
                );
                
                sendToStaff(notification);
                
                // Send simple order info separately
                String orderInfo = String.format(
                    "{\"orderId\":%d,\"orderNumber\":\"%s\",\"status\":\"%s\",\"totalPrice\":%.2f,\"tableNumber\":%d}",
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getTable() != null ? order.getTable().getNumber() : 0
                );
                
                messagingTemplate.convertAndSend("/topic/staff/orders", orderInfo);
            }
        } catch (Exception e) {
            System.out.println("WebSocket notification failed, but order was created successfully: " + e.getMessage());
            // Don't throw exception - order creation should succeed even if notification fails
        }
    }
    
    /**
     * Send order status update notification
     */
    public void sendOrderStatusUpdateNotification(Object orderData, String status) {
        try {
            if (orderData instanceof OrderEntity) {
                OrderEntity order = (OrderEntity) orderData;
                
                // Create simple notification without complex data
                NotificationDTO notification = new NotificationDTO(
                    "ORDER_STATUS_UPDATE",
                    "Order Status Updated",
                    "Order status has been updated to: " + status,
                    null, // No complex data
                    java.time.LocalDateTime.now(),
                    "MEDIUM"
                );
                
                sendToStaff(notification);
                
                // Send simple order info separately
                String orderInfo = String.format(
                    "{\"orderId\":%d,\"orderNumber\":\"%s\",\"status\":\"%s\",\"totalPrice\":%.2f,\"tableNumber\":%d}",
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getTotalPrice(),
                    order.getTable() != null ? order.getTable().getNumber() : 0
                );
                
                messagingTemplate.convertAndSend("/topic/staff/orders", orderInfo);
            }
        } catch (Exception e) {
            System.out.println("WebSocket notification failed: " + e.getMessage());
            // Don't throw exception
        }
    }
    
    /**
     * Send payment confirmed notification to table (for point claiming)
     */
    public void sendPaymentConfirmedNotification(int tableNumber, String claimToken) {
        try {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("type", "payment_confirmed");
            paymentData.put("tableNumber", tableNumber);
            paymentData.put("claimToken", claimToken);
            paymentData.put("timestamp", LocalDateTime.now().toString());
            paymentData.put("message", "Payment confirmed successfully! You can now claim your loyalty points.");
            
            // Send to specific table topic
            messagingTemplate.convertAndSend("/topic/table/" + tableNumber + "/payment", paymentData);
            
            // Also send to general table payment topic for listening
            messagingTemplate.convertAndSend("/topic/table/payment-confirmed", paymentData);
            
            System.out.println("[WEBSOCKET] Payment confirmation sent to table " + tableNumber + " with claim token: " + claimToken);
        } catch (Exception e) {
            System.err.println("[WEBSOCKET ERROR] Failed to send payment confirmation: " + e.getMessage());
        }
    }
} 