package aptech.be.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class QRCodeService {
    
    private String frontendUrl = "http://localhost:3000";
    
    /**
     * Generate QR code content for a table
     * Format: https://domain.com/order?table={table_id}
     */
    public String generateTableQRCode(int tableNumber) {
        // For now, we'll use table number in the URL
        // In production, you might want to use encrypted tokens or UUIDs
        return frontendUrl + "/order?table=" + tableNumber;
    }
    
    /**
     * Generate QR code content with table ID
     */
    public String generateTableQRCodeById(Long tableId) {
        return frontendUrl + "/order?table=" + tableId;
    }
    
    /**
     * Generate secure QR code with token (for future enhancement)
     */
    public String generateSecureTableQRCode(Long tableId) {
        String token = generateSecureToken(tableId);
        return frontendUrl + "/order?token=" + token;
    }
    
    /**
     * Generate a secure token for table access
     */
    private String generateSecureToken(Long tableId) {
        // Simple implementation - in production, use JWT or encrypted tokens
        return "table_" + tableId + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Validate if QR code content is valid
     */
    public boolean isValidQRCode(String qrContent) {
        return qrContent != null && 
               qrContent.startsWith(frontendUrl) && 
               (qrContent.contains("table=") || qrContent.contains("token="));
    }
    
    /**
     * Extract table number from QR code content
     */
    public Integer extractTableNumber(String qrContent) {
        try {
            if (qrContent.contains("table=")) {
                String tableParam = qrContent.substring(qrContent.indexOf("table=") + 6);
                // Remove any additional parameters
                if (tableParam.contains("&")) {
                    tableParam = tableParam.substring(0, tableParam.indexOf("&"));
                }
                return Integer.parseInt(tableParam);
            }
        } catch (NumberFormatException e) {
            // Log error in production
        }
        return null;
    }
} 