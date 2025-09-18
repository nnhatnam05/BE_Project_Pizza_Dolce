package aptech.be.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Profile({"default", "dev"})
public class LocalStorageService implements StorageService {

    @Value("${app.upload.base-path}")
    private String basePath;

    @Override
    public String uploadUserImage(MultipartFile file) throws Exception {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path uploadDir = Paths.get(basePath, "uploads", "users");
        Files.createDirectories(uploadDir);
        Path dest = uploadDir.resolve(fileName);
        file.transferTo(dest.toFile());
        // Trả về URL public theo FileController route
        return "/api/files/users/" + fileName;
    }
}


