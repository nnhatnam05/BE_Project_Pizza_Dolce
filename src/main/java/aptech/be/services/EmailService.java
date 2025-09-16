package aptech.be.services;

import aptech.be.models.OrderEntity;
import aptech.be.models.ComplaintCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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

    /**
     * Send a simple text email with subject and body
     */
    public void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    public void sendComplaintApprovedEmail(ComplaintCase c) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(c.getCustomer().getEmail());
            helper.setSubject("Complaint Approved - Case #" + c.getId());
            helper.setFrom("namlk0310pro@gmail.com", "DOLCE Restaurant");

            String html = buildComplaintEmailHtml(c, true, null);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception ignored) {}
    }

    public void sendComplaintRejectedEmail(ComplaintCase c, String reason) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(c.getCustomer().getEmail());
            helper.setSubject("Complaint Rejected - Case #" + c.getId());
            helper.setFrom("namlk0310pro@gmail.com", "DOLCE Restaurant");

            String html = buildComplaintEmailHtml(c, false, reason);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception ignored) {}
    }

    private String buildComplaintEmailHtml(ComplaintCase c, boolean approved, String rejectReason) {
        OrderEntity order = c.getOrder();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        String statusText = approved ? "APPROVED" : "REJECTED";
        String staffName = (c.getAssignedStaff()!=null ? c.getAssignedStaff().getName() : "CSKH");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Segoe UI,Arial,sans-serif;background:#f5f5f5;margin:0;padding:0}.wrap{max-width:640px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 4px 10px rgba(0,0,0,.05)}.header{background:")
            .append(approved?"#16a34a":"#dc2626").append(";color:#fff;padding:20px;text-align:center}.content{padding:24px}.row{display:flex;justify-content:space-between;margin:8px 0}.label{color:#6b7280}.value{color:#111827}.badge{display:inline-block;padding:6px 10px;border-radius:9999px;color:#fff;background:")
            .append(approved?"#16a34a":"#dc2626").append("}</style></head><body>");
        html.append("<div class='wrap'>");
        html.append("<div class='header'><h2>Complaint ").append(statusText).append("</h2>");
        html.append("<div class='badge'>Case #").append(c.getId()).append("</div></div>");
        html.append("<div class='content'>");
        html.append("<h3>Customer</h3>");
        html.append("<div class='row'><span class='label'>Name</span><span class='value'>").append(c.getCustomer()!=null?c.getCustomer().getFullName():"N/A").append("</span></div>");
        html.append("<div class='row'><span class='label'>Email</span><span class='value'>").append(c.getCustomer()!=null?c.getCustomer().getEmail():"N/A").append("</span></div>");

        html.append("<h3>Order</h3>");
        html.append("<div class='row'><span class='label'>Order ID</span><span class='value'>#").append(order!=null?order.getId():null).append("</span></div>");
        html.append("<div class='row'><span class='label'>Total</span><span class='value'>").append(order!=null?currencyFormat.format(order.getTotalPrice()):"N/A").append("</span></div>");
        html.append("<div class='row'><span class='label'>Payment</span><span class='value'>").append(order!=null?String.valueOf(order.getPaymentMethod()):"N/A").append("</span></div>");

        html.append("<h3>Complaint</h3>");
        html.append("<div class='row'><span class='label'>Type</span><span class='value'>").append(c.getDecisionType()!=null?c.getDecisionType():c.getType()).append("</span></div>");
        html.append("<div class='row'><span class='label'>Status</span><span class='value'>").append(statusText).append("</span></div>");
        html.append("<div class='row'><span class='label'>Customer Service</span><span class='value'>").append(staffName).append("</span></div>");
        if (c.getReason()!=null) html.append("<div class='row'><span class='label'>Reason</span><span class='value'>").append(c.getReason()).append("</span></div>");
        if (!approved) {
            html.append("<div class='row'><span class='label'>Reject Reason</span><span class='value'>").append(rejectReason!=null?rejectReason:"N/A").append("</span></div>");
        } else if (c.getRefundAmount()!=null) {
            html.append("<div class='row'><span class='label'>Refund Amount</span><span class='value'>").append(currencyFormat.format(c.getRefundAmount())).append("</span></div>");
        }
        html.append("<p style='margin-top:16px'>If you have any questions, please reply to this email.</p>");
        html.append("</div></div></body></html>");
        return html.toString();
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

    /**
     * Notify assigned CSKH staff when a new complaint/refund request is created.
     * Email includes customer/order/complaint info and a clear call to action.
     */
    public void sendComplaintCreatedEmailToStaff(ComplaintCase c) {
        try {
            if (c.getAssignedStaff() == null || c.getAssignedStaff().getEmail() == null) return;
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(c.getAssignedStaff().getEmail());
            helper.setSubject("[ACTION REQUIRED] New Complaint Case #" + c.getId());
            helper.setFrom("namlk0310pro@gmail.com", "DOLCE Restaurant");

            String html = buildStaffComplaintCreatedHtml(c);
            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (Exception ignored) {}
    }

    private String buildStaffComplaintCreatedHtml(ComplaintCase c) {
        OrderEntity order = c.getOrder();
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        String customerName = c.getCustomer()!=null?c.getCustomer().getFullName():"N/A";
        String customerEmail = c.getCustomer()!=null?c.getCustomer().getEmail():"N/A";
        String staffName = c.getAssignedStaff()!=null?c.getAssignedStaff().getName():"CSKH";

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<style>body{font-family:Segoe UI,Arial,sans-serif;background:#f5f5f5;margin:0;padding:0}.wrap{max-width:640px;margin:0 auto;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 4px 10px rgba(0,0,0,.05)}.header{background:#1f2937;color:#fff;padding:20px}.content{padding:24px}.row{display:flex;justify-content:space-between;margin:8px 0}.label{color:#6b7280}.value{color:#111827}.cta{display:inline-block;background:#2563eb;color:#fff;text-decoration:none;padding:10px 14px;border-radius:8px;margin-top:16px}</style></head><body>");
        html.append("<div class='wrap'>");
        html.append("<div class='header'><h2>New Complaint Created</h2><div>Assigned to: ").append(staffName).append("</div></div>");
        html.append("<div class='content'>");
        html.append("<h3>Customer</h3>")
            .append("<div class='row'><span class='label'>Name</span><span class='value'>").append(customerName).append("</span></div>")
            .append("<div class='row'><span class='label'>Email</span><span class='value'>").append(customerEmail).append("</span></div>");

        html.append("<h3>Order</h3>")
            .append("<div class='row'><span class='label'>Order ID</span><span class='value'>#").append(order!=null?order.getId():null).append("</span></div>")
            .append("<div class='row'><span class='label'>Total</span><span class='value'>").append(order!=null?currencyFormat.format(order.getTotalPrice()):"N/A").append("</span></div>")
            .append("<div class='row'><span class='label'>Payment</span><span class='value'>").append(order!=null?String.valueOf(order.getPaymentMethod()):"N/A").append("</span></div>");

        html.append("<h3>Complaint</h3>")
            .append("<div class='row'><span class='label'>Case #</span><span class='value'>").append(c.getId()).append("</span></div>")
            .append("<div class='row'><span class='label'>Type</span><span class='value'>").append(c.getType()).append("</span></div>")
            .append("<div class='row'><span class='label'>Reason</span><span class='value'>").append(c.getReason()!=null?c.getReason():"N/A").append("</span></div>")
            .append("<div class='row'><span class='label'>Auto Decision</span><span class='value'>").append(Boolean.TRUE.equals(c.getAutoDecisionEnabledSnapshot())?"ON":"OFF").append("</span></div>");

        html.append("<p>Please log in to the Staff Complaints Dashboard to respond to the customer as soon as possible.</p>");
        html.append("<p style='color:#ef4444'><strong>Action required:</strong> Reply to the customer in the chat and create a report/decision.</p>");
        html.append("</div></div></body></html>");
        return html.toString();
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

    /**
     * Send points earned email for dine-in claim system
     */
    public void sendPointsEarnedEmail(String to, int pointsEarned, Double orderTotal, int totalPoints) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("🎁 Congratulations! You've Earned Loyalty Points!");
            helper.setFrom("namlk0310pro@gmail.com", "DOLCE Restaurant");
            
            String htmlContent = createPointsEarnedEmailTemplate(pointsEarned, orderTotal, totalPoints);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            System.out.println("[EMAIL] Points earned email sent to: " + to);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send points earned email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String createPointsEarnedEmailTemplate(int pointsEarned, Double orderTotal, int totalPoints) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Points Earned - DOLCE Restaurant</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #FF6B35, #F7931E); color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; }");
        html.append(".content { padding: 30px; }");
        html.append(".points-section { background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 25px; border-radius: 10px; margin: 20px 0; text-align: center; }");
        html.append(".points-section h2 { margin: 0 0 15px 0; font-size: 24px; }");
        html.append(".points-earned { font-size: 36px; font-weight: bold; margin: 10px 0; }");
        html.append(".order-info { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }");
        html.append(".info-row { display: flex; justify-content: space-between; margin: 10px 0; padding: 5px 0; border-bottom: 1px solid #e9ecef; }");
        html.append(".info-label { font-weight: bold; color: #495057; }");
        html.append(".info-value { color: #212529; }");
        html.append(".rules-section { background-color: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; }");
        html.append(".rules-section h3 { margin: 0 0 15px 0; color: #1976d2; }");
        html.append(".footer { background-color: #343a40; color: white; padding: 20px; text-align: center; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>🎁 Congratulations!</h1>");
        html.append("<p>You've earned loyalty points!</p>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        
        // Points Section
        html.append("<div class='points-section'>");
        html.append("<h2>Points Earned</h2>");
        html.append("<div class='points-earned'>+").append(pointsEarned).append(" Points</div>");
        html.append("<p>Thank you for dining with us!</p>");
        html.append("</div>");
        
        // Order Info
        html.append("<div class='order-info'>");
        html.append("<h3>📋 Order Summary</h3>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Order Total:</span>");
        html.append("<span class='info-value'>").append(currencyFormat.format(orderTotal)).append("</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Points Earned:</span>");
        html.append("<span class='info-value'>").append(pointsEarned).append(" points</span>");
        html.append("</div>");
        html.append("<div class='info-row'>");
        html.append("<span class='info-label'>Your Total Points:</span>");
        html.append("<span class='info-value'>").append(totalPoints).append(" points</span>");
        html.append("</div>");
        html.append("</div>");
        
        // Rules Section
        html.append("<div class='rules-section'>");
        html.append("<h3>💡 How Points Work</h3>");
        html.append("<p><strong>Earning Rule:</strong> Every $10 spent = 10 points (rounded down)</p>");
        html.append("<p><strong>Redemption:</strong> Use your points for discounts on future orders</p>");
        html.append("<p><strong>Validity:</strong> Points never expire</p>");
        html.append("</div>");
        
        html.append("<p>Thank you for choosing DOLCE Restaurant! We look forward to serving you again.</p>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<h3>DOLCE Restaurant</h3>");
        html.append("<p>123 Main Street, Downtown, New York, NY 10001</p>");
        html.append("<p>Hotline: +1-800-DOLCE</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
