package aptech.be.controllers;

import aptech.be.models.PaymentMethod;
import aptech.be.repositories.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentMethodController {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Value("${upload.path}")
    private String uploadPath;

    @GetMapping
    public List<PaymentMethod> getAllPaymentMethods() {
        return paymentMethodRepository.findAll();
    }

    @GetMapping("/{id}")
    public PaymentMethod getPaymentMethodById(@PathVariable Long id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found: " + id));
    }


    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentMethod createPaymentMethod(
            @RequestParam("name") String name,
            @RequestParam(value = "paymentContent", required = false) String paymentContent,
            @RequestParam(value = "qrImage", required = false) MultipartFile qrImage,
            @RequestParam(value = "description") String description
    ) throws IOException {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setName(name);
        paymentMethod.setPaymentContent(paymentContent);
        paymentMethod.setDescription(description);

        if (qrImage != null && !qrImage.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + qrImage.getOriginalFilename();
            Path filePath = Paths.get(uploadPath, fileName);
            Files.createDirectories(filePath.getParent());
            Files.copy(qrImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            paymentMethod.setQrImageUrl("/uploads/paymentmethod/" + fileName);
        }

        return paymentMethodRepository.save(paymentMethod);
    }


    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentMethod updatePaymentMethod(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "paymentContent", required = false) String paymentContent,
            @RequestParam(value = "qrImage", required = false) MultipartFile qrImage,
            @RequestParam(value = "description") String description
    ) throws IOException {
        PaymentMethod old = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found: " + id));
        old.setName(name);
        old.setPaymentContent(paymentContent);
        old.setDescription(description);

        if (qrImage != null && !qrImage.isEmpty()) {

            if (old.getQrImageUrl() != null) {
                Path oldImagePath = Paths.get(uploadPath, new File(old.getQrImageUrl()).getName());
                Files.deleteIfExists(oldImagePath);
            }

            String fileName = System.currentTimeMillis() + "_" + qrImage.getOriginalFilename();
            Path filePath = Paths.get(uploadPath, fileName);
            Files.createDirectories(filePath.getParent());
            Files.copy(qrImage.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            old.setQrImageUrl("/uploads/paymentmethod/" + fileName);
        }


        return paymentMethodRepository.save(old);
    }


    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePaymentMethod(@PathVariable Long id) throws IOException {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment method not found: " + id));
        if (method.getQrImageUrl() != null) {
            Path imagePath = Paths.get(uploadPath, new File(method.getQrImageUrl()).getName());
            Files.deleteIfExists(imagePath);
        }
        paymentMethodRepository.deleteById(id);
    }
}
