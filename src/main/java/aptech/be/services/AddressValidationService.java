package aptech.be.services;

import org.springframework.stereotype.Service;

@Service
public class AddressValidationService {
    
    // Kiểm tra địa chỉ có trong nội thành HCM không
    public boolean validateHoChiMinhCityAddress(String address, Double latitude, Double longitude) {
        // Logic kiểm tra địa chỉ HCM
        if (latitude != null && longitude != null) {
            return validateHoChiMinhCityBounds(latitude, longitude);
        }
        // Nếu không có tọa độ, kiểm tra text address
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        String lowerAddress = address.toLowerCase();
        return lowerAddress.contains("ho chi minh") || 
               lowerAddress.contains("hcm") ||
               lowerAddress.contains("tp.hcm") ||
               lowerAddress.contains("thanh pho ho chi minh");
    }
    
    // Validate tọa độ trong HCM
    public boolean validateHoChiMinhCityBounds(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        // Bounds của nội thành HCM
        double minLat = 10.7;
        double maxLat = 10.9;
        double minLng = 106.6;
        double maxLng = 106.8;
        
        return latitude >= minLat && latitude <= maxLat && 
               longitude >= minLng && longitude <= maxLng;
    }
} 