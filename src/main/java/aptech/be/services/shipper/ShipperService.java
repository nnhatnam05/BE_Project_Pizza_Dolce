
package aptech.be.services.shipper;

import aptech.be.dto.shipper.ShipperDTO;
import aptech.be.models.shipper.ShipperProfile;
import aptech.be.repositories.shipper.ShipperProfileRepository;
import aptech.be.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShipperService {

    @Autowired
    private ShipperProfileRepository shipperProfileRepository;

    @Autowired
    private OrderRepository orderRepository;

    public List<ShipperDTO> getAvailableShippers() {
        return shipperProfileRepository.findAvailableShippers()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ShipperDTO> getAvailableShippersByArea(String area) {
        return shipperProfileRepository.findAvailableShippersByArea(area)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void updateLocation(Long shipperId, Double latitude, Double longitude) {
        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(shipperId);
        if (profileOpt.isPresent()) {
            ShipperProfile profile = profileOpt.get();
            profile.setCurrentLatitude(latitude);
            profile.setCurrentLongitude(longitude);
            profile.setLastLocationUpdate(LocalDateTime.now());
            shipperProfileRepository.save(profile);
        }
    }

    public void updateStatus(Long shipperId, String status) {
        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(shipperId);
        if (profileOpt.isPresent()) {
            ShipperProfile profile = profileOpt.get();
            profile.setStatus(status);
            if ("OFFLINE".equals(status)) {
                profile.setOnline(false);
                profile.setLastOnlineTime(LocalDateTime.now());
            } else if ("ACTIVE".equals(status)) {
                profile.setOnline(true);
            }
            shipperProfileRepository.save(profile);
        }
    }

    public ShipperDTO getShipperProfile(Long shipperId) {
        Optional<ShipperProfile> profileOpt = shipperProfileRepository.findByUserId(shipperId);
        return profileOpt.map(this::convertToDTO).orElse(null);
    }

    private ShipperDTO convertToDTO(ShipperProfile profile) {
        ShipperDTO dto = new ShipperDTO();
        dto.setId(profile.getId());
        dto.setUserId(profile.getUser().getId());
        dto.setUsername(profile.getUser().getUsername());
        dto.setName(profile.getUser().getName());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setVehicleType(profile.getVehicleType());
        dto.setVehicleNumber(profile.getVehicleNumber());
        dto.setStatus(profile.getStatus());
        dto.setCurrentLatitude(profile.getCurrentLatitude());
        dto.setCurrentLongitude(profile.getCurrentLongitude());
        dto.setLastLocationUpdate(profile.getLastLocationUpdate());
        dto.setTotalDeliveries(profile.getTotalDeliveries());
        dto.setWorkingArea(profile.getWorkingArea());
        dto.setOnline(profile.getOnline());
        dto.setLastOnlineTime(profile.getLastOnlineTime());

        // Tính số đơn hàng đang giao
        int activeOrders = orderRepository.findActiveOrdersByShipper(profile.getUser().getId()).size();
        dto.setActiveOrders(activeOrders);

        return dto;
    }
}