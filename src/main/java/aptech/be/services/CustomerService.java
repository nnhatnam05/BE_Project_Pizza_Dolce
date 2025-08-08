package aptech.be.services;

import aptech.be.dto.customer.CustomerSignupRequest;
import aptech.be.models.Customer;
import aptech.be.models.CustomerDetail;
import aptech.be.repositories.CustomerRepository;
import aptech.be.repositories.CustomerDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private CustomerDetailRepository customerDetailRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    private final ConcurrentHashMap<String, String> verificationCodes = new ConcurrentHashMap<>();
    @Autowired
    private EmailService emailService;

    private final ConcurrentHashMap<String, Integer> sentCount = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, LocalDateTime> blockedEmails = new ConcurrentHashMap<>();

    private final int BLOCK_MINUTES = 3;

    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email).orElse(null);
    }
    
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }


    public String register(CustomerSignupRequest req) {
        if (customerRepository.findByEmail(req.getEmail()).orElse(null) != null) {
            return "Email already registered";
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        verificationCodes.put(req.getEmail(), code);


        sendVerificationCodeEmail(req.getEmail(), code);

        return "Verification code sent to email";
    }


    public String verifyCodeAndCreateCustomer(CustomerSignupRequest req, String code) {
        String correctCode = verificationCodes.get(req.getEmail());
        if (correctCode == null) {
            return "No verification code sent to this email";
        }
        if (!correctCode.equals(code)) {
            return "Invalid verification code";
        }
        if (customerRepository.findByEmail(req.getEmail()).orElse(null) != null) {
            verificationCodes.remove(req.getEmail());
            return "Email already registered";
        }
        Customer customer = new Customer();
        customer.setFullName(req.getFullName());
        customer.setEmail(req.getEmail());
        customer.setPassword(passwordEncoder.encode(req.getPassword()));
        Customer savedCustomer = customerRepository.save(customer);
        
        // Auto-create CustomerDetail for new customer
        createDefaultCustomerDetail(savedCustomer);
        
        verificationCodes.remove(req.getEmail());
        return "Registration successful";
    }


    public String changePassword(String email, String oldPassword, String newPassword) {
        Customer customer = customerRepository.findByEmail(email).orElse(null);

        if (customer == null) {
            return "User not found";
        }
        if (!passwordEncoder.matches(oldPassword, customer.getPassword())) {
            return "Old password is incorrect";
        }
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);
        return "Password changed successfully";
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void save(Customer customer) {
        customerRepository.save(customer);
    }

    private void sendVerificationCodeEmail(String email, String code) {
        System.out.println("Sending verification code " + code + " to " + email);
    }
    public String generateAndSendVerificationCode(String email) {

        if (blockedEmails.containsKey(email) && LocalDateTime.now().isBefore(blockedEmails.get(email))) {
            return "Too many requests. Try again later!";
        }
        int count = sentCount.getOrDefault(email, 0);
        if (count >= 3) {
            blockedEmails.put(email, LocalDateTime.now().plusMinutes(BLOCK_MINUTES));
            sentCount.remove(email);
            return "Too many requests. Blocked for " + BLOCK_MINUTES + " minutes";
        }

        String code = String.format("%06d", java.util.concurrent.ThreadLocalRandom.current().nextInt(1000000));
        verificationCodes.put(email, code);
        sentCount.put(email, count + 1);
        emailService.sendVerificationCode(email, code);
        return "Verification code sent to email";
    }

    public String resetPassword(String email, String code, String newPassword) {
        String correctCode = verificationCodes.get(email);
        if (correctCode == null) {
            return "No verification code sent to this email";
        }
        if (!correctCode.equals(code)) {
            return "Invalid verification code";
        }
        Customer customer = customerRepository.findByEmail(email).orElse(null);

        if (customer == null) {
            return "User not found";
        }
        customer.setPassword(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);
        verificationCodes.remove(email);
        sentCount.remove(email);
        return "Password reset successful";
    }
    
    /**
     * Create default CustomerDetail for new customer
     */
    private void createDefaultCustomerDetail(Customer customer) {
        try {
            CustomerDetail customerDetail = new CustomerDetail();
            customerDetail.setCustomer(customer);
            customerDetail.setPoint("0"); // Start with 0 points
            customerDetail.setPhoneNumber(null); // Will be filled later
            customerDetail.setVoucher(null); // No vouchers initially
            
            // Save the customer detail
            CustomerDetail savedDetail = customerDetailRepository.save(customerDetail);
            
            // Update customer reference
            customer.setCustomerDetail(savedDetail);
            customerRepository.save(customer);
            
            System.out.println("[AUTO-CREATE SUCCESS] Created CustomerDetail for new customer: " + customer.getId());
        } catch (Exception e) {
            System.err.println("[AUTO-CREATE ERROR] Failed to create CustomerDetail for new customer: " + e.getMessage());
            // Don't throw exception here to avoid breaking registration
        }
    }

}
