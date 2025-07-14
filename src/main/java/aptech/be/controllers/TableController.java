package aptech.be.controllers;

import aptech.be.models.TableEntity;
import aptech.be.repositories.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:3000")
public class TableController {
    @Autowired
    private TableRepository tableRepository;

    @GetMapping
    public List<TableEntity> getAllTables() {
        return tableRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createTable(@RequestBody TableEntity table) {
        if (tableRepository.findByNumber(table.getNumber()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body("Table number already exists");
        }

        TableEntity saved = tableRepository.save(table);
        return ResponseEntity.ok(saved);
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> updateTable(@PathVariable Long id, @RequestBody TableEntity table) {
        return tableRepository.findById(id).map(existing -> {
            // Nếu đổi số bàn và số bàn đó đã tồn tại ở bàn khác
            if (table.getNumber() != existing.getNumber()) {
                Optional<TableEntity> conflict = tableRepository.findByNumber(table.getNumber());
                if (conflict.isPresent() && !conflict.get().getId().equals(id)) {
                    return ResponseEntity
                            .badRequest()
                            .body("Table number already exists");
                }
            }

            existing.setNumber(table.getNumber());
            existing.setCapacity(table.getCapacity());
            existing.setStatus(table.getStatus());

            return ResponseEntity.ok(tableRepository.save(existing));
        }).orElseGet(() ->
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Table not found")
        );
    }


    @DeleteMapping("/{id}")
    public void deleteTable(@PathVariable Long id) {
        tableRepository.deleteById(id);
    }

    @GetMapping("/filter")
    public List<TableEntity> filterTables(@RequestParam(required = false) String status) {
        if (status != null) {
            return tableRepository.findByStatus(status);
        } else {
            return tableRepository.findAll();
        }
    }
    @GetMapping("/{id}")
    public TableEntity getTableById(@PathVariable Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found"));
    }

}
