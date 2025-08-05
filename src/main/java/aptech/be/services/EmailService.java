package aptech.be.services;

import aptech.be.models.OrderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your Verification Code");
        message.setText("Your verification code is: " + code);
        mailSender.send(message);
    }

    public void sendPaymentSuccessEmail(String to, OrderEntity order, int pointsEarned) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("🎉 Payment Successful - Order #" + order.getOrderNumber());
            helper.setFrom("namlk0310pro@gmail.com", "DOLCE Restaurant");
            
            String htmlContent = createPaymentSuccessEmailTemplate(order, pointsEarned);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            System.out.println("[EMAIL] Payment success email sent to: " + to);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send payment success email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String createPaymentSuccessEmailTemplate(OrderEntity order, int pointsEarned) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
        
        String orderDate = order.getCreatedAt() != null ? 
            order.getCreatedAt().format(dateFormatter) : "N/A";
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Payment Successful</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; }");
        html.append(".header { background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; }");
        html.append(".content { padding: 30px; }");
        html.append(".success-icon { text-align: center; font-size: 48px; margin-bottom: 20px; }");
        html.append(".order-info { background-color: #f9f9f9; padding: 20px; border-radius: 8px; margin: 20px 0; }");
        html.append(".order-info h3 { margin-top: 0; color: #333; }");
        html.append(".info-row { display: flex; justify-content: space-between; margin: 10px 0; }");
        html.append(".info-label { font-weight: bold; color: #666; }");
        html.append(".info-value { color: #333; }");
        html.append(".points-section { background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; }");
        html.append(".points-section h3 { margin: 0 0 10px 0; }");
        html.append(".points-earned { font-size: 24px; font-weight: bold; }");
        html.append(".reminder { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 8px; margin: 20px 0; }");
        html.append(".reminder h4 { margin-top: 0; color: #856404; }");
        html.append(".footer { background-color: #333; color: white; padding: 20px; text-align: center; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎉 Payment Successful!</h1>");
        html.append("<p>Thank you for your order at DOLCE Restaurant</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        html.append("<div class='success-icon'>✅</div>");
        html.append("<p>Dear ").append(order.getCustomer().getFullName()).append(",</p>");
        html.append("<p>We're excited to confirm that your payment has been processed successfully! Your delicious order is now being prepared with care.</p>");
        
        // Order Information
        html.append("<div class='order-info'>");
        html.append("<h3>📋 Order Details</h3>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Order Number:</span>");
        html.append("<span class='info-value'>").append(order.getOrderNumber()).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Order Date:</span>");
        html.append("<span class='info-value'>").append(orderDate).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Total Amount:</span>");
        html.append("<span class='info-value'>").append(currencyFormat.format(order.getTotalPrice())).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Payment Status:</span>");
        html.append("<span class='info-value' style='color: #4CAF50; font-weight: bold;'>PAID</span>");
        html.append("</div>");
        html.append("</div>");
        
        // Points Section
        if (pointsEarned > 0) {
            html.append("<div class='points-section'>");
            html.append("<h3>🎁 Congratulations! You've Earned Points!</h3>");
            html.append("<div class='points-earned'>+").append(pointsEarned).append(" Points</div>");
            html.append("<p>You earned ").append(pointsEarned).append(" loyalty points from this order!</p>");
            html.append("<p style='font-size: 14px; opacity: 0.9;'>Rule: Every $10 = 10 points (rounded down)</p>");
            html.append("</div>");
        }
        
        // Delivery Reminder
        html.append("<div class='reminder'>");
        html.append("<h4>📦 Important Delivery Information</h4>");
        html.append("<ul>");
        html.append("<li><strong>Please be available</strong> to receive your order during delivery hours</li>");
        html.append("<li><strong>Check your phone</strong> - our delivery team will contact you before arrival</li>");
        html.append("<li><strong>Prepare exact change</strong> if you're paying cash on delivery</li>");
        html.append("<li><strong>Verify your order</strong> upon delivery to ensure everything is correct</li>");
        html.append("</ul>");
        html.append("<p><strong>Estimated delivery time:</strong> 30-45 minutes from order confirmation</p>");
        html.append("</div>");
        
        html.append("<p>If you have any questions or concerns about your order, please don't hesitate to contact us at <strong>+1-800-DOLCE</strong>.</p>");
        html.append("<p>Thank you for choosing DOLCE Restaurant!</p>");
        html.append("<p>Best regards,<br><strong>The DOLCE Team</strong></p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>&copy; 2025 DOLCE Restaurant. All rights reserved.</p>");
        html.append("<p>123 Main Street, Downtown, New York, NY 10001</p>");
        html.append("<p>Hotline: +1-800-DOLCE</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
