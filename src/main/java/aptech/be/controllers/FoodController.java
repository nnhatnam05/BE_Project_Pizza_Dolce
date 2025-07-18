package aptech.be.controllers;

import aptech.be.models.Food;
import aptech.be.repositories.FoodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/foods")
@CrossOrigin(origins = "http://localhost:3000")
public class FoodController {

    @Autowired
    private FoodRepository foodRepo;

    @GetMapping
    public List<Food> getAllFoods() {
        return foodRepo.findAll();
    }

    @PostMapping(value = "/create" , consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Food createFood(
            @RequestParam("name") String name,
            @RequestParam("price") double price,
            @RequestParam("description") String description,
            @RequestParam("status") String status,
            @RequestParam("type")  String type,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        try {
            // Kiểm tra name trùng
            if (foodRepo.findByName(name).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food name already exists");
            }

            Food food = new Food();
            food.setName(name);
            food.setPrice(price);
            food.setDescription(description);
            food.setStatus(status);
            food.setType(type);

            if (imageFile != null && !imageFile.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                String uploadDir = System.getProperty("user.dir") + "/uploads";
                Path uploadPath = Paths.get(uploadDir);

                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                imageFile.transferTo(filePath.toFile());

                String imageUrl = "/uploads/" + fileName;

                // Kiểm tra image trùng
                if (foodRepo.findByImageUrl(imageUrl).isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image already exists");
                }

                food.setImageUrl(imageUrl);
            }

            return foodRepo.save(food);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image", e);
        }
    }



    @PostMapping(value = "/update/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public Food updateFood(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("price") double price,
            @RequestParam("description") String description,
            @RequestParam("status") String status,
            @RequestParam("type") String type,
            @RequestParam(value = "image", required = false) MultipartFile imageFile
    ) {
        Food existing = foodRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));


        if (!existing.getName().equals(name) && foodRepo.findByName(name).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Food name already exists");
        }

        existing.setName(name);
        existing.setPrice(price);
        existing.setDescription(description);
        existing.setStatus(status);
        existing.setType(type);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = System.currentTimeMillis() + "_" + imageFile.getOriginalFilename();
                String imageUrl = "/uploads/" + fileName;

                // Nếu image mới trùng với image của food khác
                if (foodRepo.findByImageUrl(imageUrl).isPresent()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image already exists");
                }

                // Xóa ảnh cũ nếu có
                if (existing.getImageUrl() != null) {
                    String oldImagePath = System.getProperty("user.dir") + existing.getImageUrl().replace("/uploads", "/uploads");
                    File oldImageFile = new File(oldImagePath);
                    if (oldImageFile.exists()) {
                        oldImageFile.delete();
                    }
                }

                String uploadDir = System.getProperty("user.dir") + "/uploads";
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path filePath = uploadPath.resolve(fileName);
                imageFile.transferTo(filePath.toFile());

                existing.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image", e);
            }
        }

        return foodRepo.save(existing);
    }




    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteFood(@PathVariable Long id) {
        Food food = foodRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));

        if (food.getImageUrl() != null) {
            String imageFileName = food.getImageUrl().replace("/uploads/", "");
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            File imageFile = new File(uploadDir, imageFileName);
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }

        foodRepo.deleteById(id);
    }



    @GetMapping("/filter")
    public List<Food> filterFoods(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type
    ) {
        if (status != null) {
            return foodRepo.findByStatus(status);
        } else if (name != null) {
            return foodRepo.findByNameContainingIgnoreCase(name);
        }else if (type != null) {
            return foodRepo.findByType(type);
        } else {
            return foodRepo.findAll();
        }
    }

    @GetMapping("/{id}")
    public Food getFoodById(@PathVariable Long id) {
        return foodRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));
    }
}
