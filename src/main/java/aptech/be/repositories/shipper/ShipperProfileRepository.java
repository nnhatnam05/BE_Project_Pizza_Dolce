// be/src/main/java/aptech/be/repositories/staff/ShipperProfileRepository.java
package aptech.be.repositories.shipper;

import aptech.be.models.shipper.ShipperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperProfileRepository extends JpaRepository<ShipperProfile, Long> {
    Optional<ShipperProfile> findByUserId(Long userId);
    List<ShipperProfile> findByStatus(String status);
    List<ShipperProfile> findByIsOnlineTrue();
    List<ShipperProfile> findByStatusAndIsOnlineTrue(String status);
    List<ShipperProfile> findByWorkingAreaContaining(String area);

    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.isOnline = true AND sp.status = 'ACTIVE'")
    List<ShipperProfile> findAvailableShippers();

    @Query("SELECT sp FROM ShipperProfile sp WHERE sp.isOnline = true AND sp.status = 'ACTIVE' AND sp.workingArea LIKE %:area%")
    List<ShipperProfile> findAvailableShippersByArea(@Param("area") String area);
}