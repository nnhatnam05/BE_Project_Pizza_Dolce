package aptech.be.services;

import aptech.be.dto.customer.CustomerSignupRequest;
import aptech.be.models.Customer;
import aptech.be.repositories.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerService{

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Customer findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public String register(CustomerSignupRequest req) {
        if (customerRepository.findByEmail(req.getEmail()) != null) {
            return "Email already registered";
        }
        Customer customer = new Customer();
        customer.setFullName(req.getFullName());
        customer.setEmail(req.getEmail());
        customer.setPassword(passwordEncoder.encode(req.getPassword()));
        customerRepository.save(customer);
        return "Registration successful";
    }

    public String changePassword(String email, String oldPassword, String newPassword) {
        Customer customer = customerRepository.findByEmail(email);
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

}
