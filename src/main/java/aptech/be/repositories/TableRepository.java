package aptech.be.repositories;

import aptech.be.models.TableEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableRepository extends JpaRepository<TableEntity, Long> {
    List<TableEntity> findByStatus(String status);
    Optional<TableEntity> findByNumber(int number);


}
