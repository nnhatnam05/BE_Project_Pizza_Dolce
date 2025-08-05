package aptech.be.services;

import aptech.be.models.OrderEntity;
import aptech.be.models.OrderFood;
import aptech.be.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceEmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private OrderRepository orderRepository;

    public void sendInvoiceEmail(OrderEntity order) {
        if (order.getNeedInvoice() == null || !order.getNeedInvoice()) {
            return; // Không cần gửi hóa đơn
        }

        if (order.getInvoiceSent() != null && order.getInvoiceSent()) {
            return; // Đã gửi rồi
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(order.getCustomer().getEmail());
            helper.setSubject("Hóa đơn điện tử - Đơn hàng #" + order.getOrderNumber());
            helper.setFrom("noreply@restaurant.com");

            String htmlContent = generateInvoiceHtml(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            // Đánh dấu đã gửi hóa đơn và save vào database
            order.setInvoiceSent(true);
            orderRepository.save(order);
            
            System.out.println("[INVOICE] Electronic invoice sent successfully to: " + order.getCustomer().getEmail());
            
        } catch (MessagingException e) {
            System.err.println("Failed to send invoice email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generateInvoiceHtml(OrderEntity order) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Hóa đơn điện tử</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append(".invoice-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 0 10px rgba(0,0,0,0.1); }");
        html.append(".header { text-align: center; border-bottom: 2px solid #007bff; padding-bottom: 20px; margin-bottom: 30px; }");
        html.append(".header h1 { color: #007bff; margin: 0; font-size: 28px; }");
        html.append(".header p { color: #666; margin: 5px 0; }");
        html.append(".invoice-info { display: flex; justify-content: space-between; margin-bottom: 30px; }");
        html.append(".info-section h3 { color: #333; margin-bottom: 10px; font-size: 16px; }");
        html.append(".info-section p { margin: 5px 0; color: #666; }");
        html.append(".items-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }");
        html.append(".items-table th { background: #007bff; color: white; padding: 12px; text-align: left; }");
        html.append(".items-table td { padding: 12px; border-bottom: 1px solid #ddd; }");
        html.append(".items-table tr:nth-child(even) { background: #f9f9f9; }");
        html.append(".total-section { text-align: right; margin-top: 20px; }");
        html.append(".total-row { display: flex; justify-content: space-between; margin: 5px 0; }");
        html.append(".total-final { font-size: 18px; font-weight: bold; color: #007bff; border-top: 2px solid #007bff; padding-top: 10px; }");
        html.append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #666; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        
        html.append("<div class='invoice-container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>HÓA ĐƠN ĐIỆN TỬ</h1>");
        html.append("<p>Restaurant Management System</p>");
        html.append("<p>Đơn hàng #").append(order.getOrderNumber()).append("</p>");
        html.append("</div>");
        
        // Invoice Info
        html.append("<div class='invoice-info'>");
        html.append("<div class='info-section'>");
        html.append("<h3>Thông tin khách hàng:</h3>");
        html.append("<p><strong>Tên:</strong> ").append(order.getCustomer().getFullName()).append("</p>");
        html.append("<p><strong>Email:</strong> ").append(order.getCustomer().getEmail()).append("</p>");
        if (order.getCustomer().getCustomerDetail() != null && order.getCustomer().getCustomerDetail().getPhoneNumber() != null) {
            html.append("<p><strong>Điện thoại:</strong> ").append(order.getCustomer().getCustomerDetail().getPhoneNumber()).append("</p>");
        }
        html.append("</div>");
        
        html.append("<div class='info-section'>");
        html.append("<h3>Thông tin giao hàng:</h3>");
        html.append("<p><strong>Người nhận:</strong> ").append(order.getRecipientName()).append("</p>");
        html.append("<p><strong>Điện thoại:</strong> ").append(order.getRecipientPhone()).append("</p>");
        html.append("<p><strong>Địa chỉ:</strong> ").append(order.getDeliveryAddress()).append("</p>");
        html.append("</div>");
        
        html.append("<div class='info-section'>");
        html.append("<h3>Thông tin đơn hàng:</h3>");
        html.append("<p><strong>Ngày đặt:</strong> ").append(order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</p>");
        html.append("<p><strong>Trạng thái:</strong> ").append(order.getStatus()).append("</p>");
        if (order.getNote() != null && !order.getNote().trim().isEmpty()) {
            html.append("<p><strong>Ghi chú:</strong> ").append(order.getNote()).append("</p>");
        }
        html.append("</div>");
        html.append("</div>");
        
        // Items Table
        html.append("<table class='items-table'>");
        html.append("<thead>");
        html.append("<tr>");
        html.append("<th>Món ăn</th>");
        html.append("<th>Đơn giá</th>");
        html.append("<th>Số lượng</th>");
        html.append("<th>Thành tiền</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        double subtotal = 0;
        for (OrderFood orderFood : order.getOrderFoods()) {
            double price = orderFood.getFood().getPrice();
            double itemTotal = price * orderFood.getQuantity();
            subtotal += itemTotal;
            
            html.append("<tr>");
            html.append("<td>").append(orderFood.getFood().getName()).append("</td>");
            html.append("<td>$").append(String.format("%.2f", price)).append("</td>");
            html.append("<td>").append(orderFood.getQuantity()).append("</td>");
            html.append("<td>$").append(String.format("%.2f", itemTotal)).append("</td>");
            html.append("</tr>");
        }
        
        html.append("</tbody>");
        html.append("</table>");
        
        // Total Section
        html.append("<div class='total-section'>");
        html.append("<div class='total-row'>");
        html.append("<span>Tạm tính:</span>");
        html.append("<span>$").append(String.format("%.2f", subtotal)).append("</span>");
        html.append("</div>");
        
        html.append("<div class='total-row'>");
        html.append("<span>Phí giao hàng:</span>");
        html.append("<span>Miễn phí</span>");
        html.append("</div>");
        
        if (order.getVoucherCode() != null && order.getVoucherDiscount() != null && order.getVoucherDiscount() > 0) {
            html.append("<div class='total-row' style='color: #4CAF50;'>");
            html.append("<span>Giảm giá (").append(order.getVoucherCode()).append("):</span>");
            html.append("<span>-$").append(String.format("%.2f", order.getVoucherDiscount())).append("</span>");
            html.append("</div>");
        }
        
        html.append("<div class='total-row total-final'>");
        html.append("<span>Tổng cộng:</span>");
        html.append("<span>$").append(String.format("%.2f", order.getTotalPrice())).append("</span>");
        html.append("</div>");
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!</p>");
        html.append("<p>Đây là hóa đơn điện tử được tạo tự động, vui lòng không trả lời email này.</p>");
        html.append("<p>Mọi thắc mắc xin liên hệ: support@restaurant.com | Hotline: 1900-xxxx</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
} 