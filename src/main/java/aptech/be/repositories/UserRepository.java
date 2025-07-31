package aptech.be.repositories;

import aptech.be.models.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByPhone(String phone);
    List<UserEntity> findByRole(String role);
    Optional<UserEntity> findByImageUrlContaining(String keyword);



}

