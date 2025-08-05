package aptech.be.controllers;

import aptech.be.repositories.FoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class CategoryController {

    @Autowired
    private FoodRepository foodRepository;

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories() {
        try {
            // Get unique food types from database
            List<String> uniqueTypes = foodRepository.findAll()
                    .stream()
                    .map(food -> food.getType())
                    .filter(type -> type != null && !type.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            // Convert to category format expected by frontend
            List<Map<String, Object>> categories = uniqueTypes.stream()
                    .map(type -> {
                        Map<String, Object> category = Map.of(
                                "id", (Object) type,
                                "name", (Object) (type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase())
                        );
                        return category;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of()); // Return empty list on error
        }
    }
} 