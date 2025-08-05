
package aptech.be.dto.shipper;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ShipperDTO {
    private Long id;
    private Long userId;
    private String username;
    private String name;
    private String phoneNumber;
    private String vehicleType;
    private String vehicleNumber;
    private String status;
    private Double currentLatitude;
    private Double currentLongitude;
    private LocalDateTime lastLocationUpdate;
    private Integer totalDeliveries;
    private Double rating;
    private String workingArea;
    private Boolean isOnline;
    private LocalDateTime lastOnlineTime;
    private Integer activeOrders; // Số đơn hàng đang giao

    public ShipperDTO() {
    }

    public ShipperDTO(Long id, Long userId, String username, String name, String phoneNumber, String vehicleType, String vehicleNumber, String status, Double currentLatitude, Double currentLongitude, LocalDateTime lastLocationUpdate, Integer totalDeliveries, Double rating, String workingArea, Boolean isOnline, LocalDateTime lastOnlineTime, Integer activeOrders) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.status = status;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.lastLocationUpdate = lastLocationUpdate;
        this.totalDeliveries = totalDeliveries;
        this.rating = rating;
        this.workingArea = workingArea;
        this.isOnline = isOnline;
        this.lastOnlineTime = lastOnlineTime;
        this.activeOrders = activeOrders;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public LocalDateTime getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) {
        this.lastLocationUpdate = lastLocationUpdate;
    }

    public Integer getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(Integer totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getWorkingArea() {
        return workingArea;
    }

    public void setWorkingArea(String workingArea) {
        this.workingArea = workingArea;
    }

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }

    public LocalDateTime getLastOnlineTime() {
        return lastOnlineTime;
    }

    public void setLastOnlineTime(LocalDateTime lastOnlineTime) {
        this.lastOnlineTime = lastOnlineTime;
    }

    public Integer getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(Integer activeOrders) {
        this.activeOrders = activeOrders;
    }
}