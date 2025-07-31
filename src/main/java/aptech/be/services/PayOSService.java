package aptech.be.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class PayOSService {
    
    @Value("${payos.clientId}")
    private String clientId;
    
    @Value("${payos.apiKey}")
    private String apiKey;
    
    @Value("${payos.checksumKey}")
    private String checksumKey;
    
    @Value("${payos.returnUrl}")
    private String returnUrl;
    
    @Value("${payos.cancelUrl}")
    private String cancelUrl;
    
    private RestTemplate restTemplate;
    
    public PayOSService() {
        this.restTemplate = new RestTemplate();
        
        // Cấu hình timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000); // 30 seconds
        this.restTemplate.setRequestFactory(factory);
    }
    
    @PostConstruct
    public void init() {
        System.out.println("[PAYOS] Service initialized with ClientId: " + clientId);
    }

    public String createPaymentUrl(Long orderId, int amount, String orderInfo) {
        try {
            // orderCode là SỐ hoặc CHUỖI DUY NHẤT, vẫn nên để là số thời gian cho test local
            long orderCode = System.currentTimeMillis();

            // Tạo chuỗi key=value theo thứ tự alphabet
            Map<String, String> params = new HashMap<>();
            params.put("amount", String.valueOf(amount));
            params.put("cancelUrl", cancelUrl);
            params.put("description", orderInfo);
            params.put("orderCode", String.valueOf(orderCode));
            params.put("returnUrl", returnUrl);
            
            // Sắp xếp theo alphabet và tạo chuỗi key=value&key=value
            String raw = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "&" + b);
            
            // Ký bằng HMAC_SHA256
            String signature = hmacSha256(raw, checksumKey);

            System.out.println("[PAYOS] Raw signature: " + raw);
            System.out.println("[PAYOS] Generated signature: " + signature);

            // Body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderCode", orderCode);
            requestBody.put("amount", amount);
            requestBody.put("description", orderInfo);
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("cancelUrl", cancelUrl);
            requestBody.put("signature", signature);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.set("ngrok-skip-browser-warning", "true");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String payosUrl = "https://api-merchant.payos.vn/v2/payment-requests";

            System.out.println("[PAYOS] Request URL: " + payosUrl);
            System.out.println("[PAYOS] Request Headers: " + headers);
            System.out.println("[PAYOS] Request Body: " + requestBody);

            try {
                Map<String, Object> response = restTemplate.postForObject(payosUrl, request, Map.class);
                System.out.println("[PAYOS] Response: " + response);

                if (response != null && "00".equals(response.get("code"))) {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    String checkoutUrl = (String) data.get("checkoutUrl");
                    System.out.println("[PAYOS] Payment URL created successfully: " + checkoutUrl);
                    return checkoutUrl;
                } else {
                    String errorMsg = response != null ? response.get("desc").toString() : "Unknown error";
                    System.err.println("[PAYOS] Error creating payment URL: " + errorMsg);
                    throw new RuntimeException("PayOS error: " + errorMsg);
                }
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                System.err.println("[PAYOS] HTTP Error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
                throw new RuntimeException("PayOS HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            } catch (Exception e) {
                System.err.println("[PAYOS] Exception: " + e.getMessage());
                throw new RuntimeException("PayOS exception: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("[PAYOS] Exception creating payment URL: " + e.getMessage());
            throw new RuntimeException("Lỗi tạo URL thanh toán PayOS: " + e.getMessage());
        }
    }


    public boolean verifyWebhook(Map<String, Object> webhookData, String signature) {
        try {
            // Implement webhook verification logic here
            // For now, return true for testing
            System.out.println("[PAYOS] Webhook verification: " + webhookData);
            return true;
        } catch (Exception e) {
            System.err.println("[PAYOS] Error verifying webhook: " + e.getMessage());
            return false;
        }
    }
    
        public Map<String, String> getPaymentStatus(String orderCode) {
        try {
        HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            HttpEntity<String> request = new HttpEntity<>(headers);

            // Endpoint mới PayOS 2024
            String payosUrl = "https://api-merchant.payos.vn/v2/payment-requests/" + orderCode;
            Map<String, Object> response = restTemplate.getForObject(payosUrl, Map.class);

            Map<String, String> result = new HashMap<>();
            if (response != null && "00".equals(response.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                result.put("status", (String) data.get("status"));
                result.put("amount", String.valueOf(data.get("amount")));
                result.put("description", (String) data.get("description"));
                        } else {
                result.put("status", "ERROR");
                result.put("error", response != null ? response.get("desc").toString() : "Unknown error");
            }
            return result;
        } catch (Exception e) {
            System.err.println("[PAYOS] Error getting payment status: " + e.getMessage());
            Map<String, String> result = new HashMap<>();
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
            return result;
                }
    }
    
    public String createPaymentUrlWithBuyer(Long orderId, int amount, String orderInfo, String buyerName, String buyerEmail, String buyerPhone, String buyerAddress) {
        try {
            long orderCode = System.currentTimeMillis();
            // Các trường cần thiết cho PayOS
            Map<String, String> params = new HashMap<>();
            params.put("amount", String.valueOf(amount));
            params.put("cancelUrl", cancelUrl);
            params.put("description", orderInfo);
            params.put("orderCode", String.valueOf(orderCode));
            params.put("returnUrl", returnUrl);
            // Không đưa các trường buyer vào chuỗi ký signature

            String raw = params.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "&" + b);

            String signature = hmacSha256(raw, checksumKey);

            // Build body gửi sang PayOS (có thêm buyer info)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("orderCode", orderCode);
            requestBody.put("amount", amount);
            requestBody.put("description", orderInfo);
            requestBody.put("returnUrl", returnUrl);
            requestBody.put("cancelUrl", cancelUrl);
            requestBody.put("signature", signature);
            requestBody.put("buyerName", buyerName);
            requestBody.put("buyerEmail", buyerEmail);
            requestBody.put("buyerPhone", buyerPhone);
            requestBody.put("buyerAddress", buyerAddress);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);
            headers.set("ngrok-skip-browser-warning", "true");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String payosUrl = "https://api-merchant.payos.vn/v2/payment-requests";
            Map<String, Object> response = restTemplate.postForObject(payosUrl, request, Map.class);

            if (response != null && "00".equals(response.get("code"))) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                String checkoutUrl = (String) data.get("checkoutUrl");
                return checkoutUrl;
            } else {
                String errorMsg = response != null ? response.get("desc").toString() : "Unknown error";
                throw new RuntimeException("PayOS error: " + errorMsg);
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL thanh toán PayOS: " + e.getMessage());
        }
    }
    
    // Hàm tạo SHA256 hash
    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // Hàm tạo HMAC_SHA256
    private String hmacSha256(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }
} 