package aptech.be.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TableSessionService {
    
    @Autowired
    private WebSocketNotificationService notificationService;
    
    // In-memory storage for table sessions (in production, use Redis or database)
    private final Map<Long, TableSession> tableSessions = new ConcurrentHashMap<>();
    private final Map<Long, StaffCall> pendingStaffCalls = new ConcurrentHashMap<>();
    private final Map<Long, PaymentRequest> pendingPaymentRequests = new ConcurrentHashMap<>();
    
    /**
     * Create a staff call for a table (with table number)
     */
    public void createStaffCall(Long tableId, String reason, int tableNumber) {
        StaffCall staffCall = new StaffCall(tableId, reason, LocalDateTime.now());
        pendingStaffCalls.put(tableId, staffCall);
        
        // Send real-time notification to staff dashboard
        notificationService.sendStaffCallNotification(tableId, reason, tableNumber);
        
        System.out.println("Staff call created for table " + tableId + " (Table " + tableNumber + ") at " + LocalDateTime.now());
    }
    
    /**
     * Create a staff call for a table (backward compatibility)
     */
    public void createStaffCall(Long tableId, String reason) {
        // Default to unknown table number for backward compatibility
        createStaffCall(tableId, reason, 0);
    }
    
    /**
     * Auto-dismiss staff calls after 1 minute
     * Runs every 30 seconds to check for expired calls
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void autoDismissExpiredStaffCalls() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteAgo = now.minusMinutes(1);
        
        pendingStaffCalls.entrySet().removeIf(entry -> {
            StaffCall call = entry.getValue();
            if (call.getCallTime().isBefore(oneMinuteAgo)) {
                System.out.println("Auto-dismissing expired staff call for table " + entry.getKey() + 
                                 " (created at " + call.getCallTime() + ")");
                
                // TODO: Send notification to remove the call from staff dashboard
                // notificationService.sendStaffCallDismissed(entry.getKey());
                
                return true; // Remove this entry
            }
            return false; // Keep this entry
        });
    }
    
    /**
     * Create a payment request for a table (with table number)
     */
    public void createPaymentRequest(Long tableId, int tableNumber) {
        PaymentRequest paymentRequest = new PaymentRequest(tableId, LocalDateTime.now());
        pendingPaymentRequests.put(tableId, paymentRequest);
        
        // Send real-time notification to staff dashboard
        notificationService.sendPaymentRequestNotification(tableId, tableNumber);
    }
    
    /**
     * Create a payment request for a table (backward compatibility)
     */
    public void createPaymentRequest(Long tableId) {
        // Default to unknown table number for backward compatibility
        createPaymentRequest(tableId, 0);
    }
    
    /**
     * Get table session information
     */
    public Map<String, Object> getTableSessionInfo(Long tableId) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        TableSession session = tableSessions.get(tableId);
        if (session != null) {
            sessionInfo.put("sessionId", session.getSessionId());
            sessionInfo.put("startTime", session.getStartTime());
        }
        
        StaffCall staffCall = pendingStaffCalls.get(tableId);
        if (staffCall != null) {
            sessionInfo.put("hasPendingStaffCall", true);
            sessionInfo.put("staffCallReason", staffCall.getReason());
            sessionInfo.put("staffCallTime", staffCall.getCallTime());
        } else {
            sessionInfo.put("hasPendingStaffCall", false);
        }
        
        PaymentRequest paymentRequest = pendingPaymentRequests.get(tableId);
        if (paymentRequest != null) {
            sessionInfo.put("hasPendingPaymentRequest", true);
            sessionInfo.put("paymentRequestTime", paymentRequest.getRequestTime());
        } else {
            sessionInfo.put("hasPendingPaymentRequest", false);
        }
        
        return sessionInfo;
    }
    
    /**
     * Get all pending staff calls (for staff dashboard)
     */
    public Map<Long, StaffCall> getAllPendingStaffCalls() {
        return new HashMap<>(pendingStaffCalls);
    }
    
    /**
     * Get all pending payment requests (for staff dashboard)
     */
    public Map<Long, PaymentRequest> getAllPendingPaymentRequests() {
        return new HashMap<>(pendingPaymentRequests);
    }
    
    /**
     * Resolve staff call
     */
    public void resolveStaffCall(Long tableId) {
        pendingStaffCalls.remove(tableId);
    }
    
    /**
     * Resolve payment request
     */
    public void resolvePaymentRequest(Long tableId) {
        pendingPaymentRequests.remove(tableId);
    }
    
    /**
     * Create or update table session
     */
    public void createTableSession(Long tableId, String sessionId) {
        TableSession session = new TableSession(sessionId, tableId, LocalDateTime.now());
        tableSessions.put(tableId, session);
    }
    
    /**
     * End table session
     */
    public void endTableSession(Long tableId) {
        tableSessions.remove(tableId);
        pendingStaffCalls.remove(tableId);
        pendingPaymentRequests.remove(tableId);
    }

    public void endSession(Long tableId) {
        // Remove any pending staff calls and payment requests
        pendingStaffCalls.remove(tableId);
        pendingPaymentRequests.remove(tableId);
        
        // Clear the table session
        tableSessions.remove(tableId);
        
        System.out.println("Table session ended for table: " + tableId);
    }
    
    // Private methods for notifications (implement with WebSocket/SSE in production)
    private void notifyStaff(StaffCall staffCall) {
        // TODO: Implement real-time notification to staff dashboard
        System.out.println("Staff notification: Table " + staffCall.getTableId() + " needs assistance - " + staffCall.getReason());
    }
    
    private void notifyStaffForPayment(PaymentRequest paymentRequest) {
        // TODO: Implement real-time notification to staff dashboard
        System.out.println("Payment notification: Table " + paymentRequest.getTableId() + " requests payment");
    }
    
    // Inner classes for data structures
    public static class TableSession {
        private String sessionId;
        private Long tableId;
        private LocalDateTime startTime;
        
        public TableSession(String sessionId, Long tableId, LocalDateTime startTime) {
            this.sessionId = sessionId;
            this.tableId = tableId;
            this.startTime = startTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public Long getTableId() { return tableId; }
        public LocalDateTime getStartTime() { return startTime; }
    }
    
    public static class StaffCall {
        private Long tableId;
        private String reason;
        private LocalDateTime callTime;
        
        public StaffCall(Long tableId, String reason, LocalDateTime callTime) {
            this.tableId = tableId;
            this.reason = reason;
            this.callTime = callTime;
        }
        
        // Getters
        public Long getTableId() { return tableId; }
        public String getReason() { return reason; }
        public LocalDateTime getCallTime() { return callTime; }
    }
    
    public static class PaymentRequest {
        private Long tableId;
        private LocalDateTime requestTime;
        
        public PaymentRequest(Long tableId, LocalDateTime requestTime) {
            this.tableId = tableId;
            this.requestTime = requestTime;
        }
        
        // Getters
        public Long getTableId() { return tableId; }
        public LocalDateTime getRequestTime() { return requestTime; }
    }
} 