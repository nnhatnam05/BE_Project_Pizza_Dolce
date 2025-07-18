package aptech.be.services;

import aptech.be.models.UserEntity;
import aptech.be.repositories.UserRepository;
import aptech.be.models.Customer;
import aptech.be.models.CustomerDetail;
import aptech.be.repositories.CustomerDetailRepository;
import aptech.be.repositories.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDetailRepository customerDetailRepository;

    @Autowired
    public UserDetailsServiceImpl(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            CustomerDetailRepository customerDetailRepository
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.customerDetailRepository = customerDetailRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Tìm trong user (admin/staff) trước
        System.out.println("Find user by: " + usernameOrEmail);

        UserEntity user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElse(null);

        if (user != null) {
            System.out.println("USER LOGIN: " + user.getUsername());
            return new CustomUserDetails(user);
        }

        // Tìm trong customer (bằng email)
        Customer customer = customerRepository.findByEmail(usernameOrEmail);
        if (customer != null) {
            System.out.println("CUSTOMER LOGIN: " + customer.getEmail());
            return new CustomerDetails(customer);
        }

        throw new UsernameNotFoundException("User or Customer not found");
    }

    // ----------- Gộp CRUD cho CustomerDetail --------------------
    public CustomerDetail saveCustomerDetail(CustomerDetail detail) {
        return customerDetailRepository.save(detail);
    }

    public Optional<CustomerDetail> findCustomerDetailById(Long id) {
        return customerDetailRepository.findById(id);
    }

    public CustomerDetail findCustomerDetailByCustomerId(Long customerId) {
        return customerDetailRepository.findByCustomerId(customerId);
    }

    public void deleteCustomerDetail(Long id) {
        customerDetailRepository.deleteById(id);
    }
}
