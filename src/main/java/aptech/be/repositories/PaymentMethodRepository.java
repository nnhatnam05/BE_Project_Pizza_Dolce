package aptech.be.repositories;


import aptech.be.models.OrderEntity;
import aptech.be.models.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod,Long> {
    PaymentMethod findByName(String name);

}
