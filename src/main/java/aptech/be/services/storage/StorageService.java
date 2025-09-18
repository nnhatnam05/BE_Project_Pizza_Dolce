package aptech.be.services.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadUserImage(MultipartFile file) throws Exception;
}


