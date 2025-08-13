package aptech.be.repositories;

import aptech.be.models.Food;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FoodRepository extends JpaRepository<Food,Long> {
    List<Food> findByStatus(String status);
    List<Food> findByNameContainingIgnoreCase(String keyword);
    Optional<Food> findByName(String name);
    Optional<Food> findByImageUrl(String imageUrl);
    List<Food> findByType(String type);
    
    // Dashboard Analytics Methods
    long countByStatus(String status);
}
