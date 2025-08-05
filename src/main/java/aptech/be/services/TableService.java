package aptech.be.services;

import aptech.be.models.TableEntity;
import aptech.be.repositories.TableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TableService {
    
    @Autowired
    private TableRepository tableRepository;
    
    @Autowired
    private QRCodeService qrCodeService;
    
    // Table status constants
    public static final String STATUS_AVAILABLE = "AVAILABLE";
    public static final String STATUS_OCCUPIED = "OCCUPIED";
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CLEANING = "CLEANING";
    
    public List<TableEntity> getAllTables() {
        return tableRepository.findAll();
    }
    
    public Optional<TableEntity> getTableById(Long id) {
        return tableRepository.findById(id);
    }
    
    public List<TableEntity> getTablesByStatus(String status) {
        return tableRepository.findByStatus(status);
    }
    
    public TableEntity createTable(TableEntity table) {
        // Set default status if not provided
        if (table.getStatus() == null || table.getStatus().isEmpty()) {
            table.setStatus(STATUS_AVAILABLE);
        }
        
        // Generate QR code for the table
        String qrCode = qrCodeService.generateTableQRCode(table.getNumber());
        table.setQrCode(qrCode);
        
        return tableRepository.save(table);
    }
    
    public TableEntity updateTable(Long id, TableEntity tableData) {
        return tableRepository.findById(id)
                .map(existingTable -> {
                    existingTable.setNumber(tableData.getNumber());
                    existingTable.setCapacity(tableData.getCapacity());
                    existingTable.setStatus(tableData.getStatus());
                    existingTable.setLocation(tableData.getLocation());
                    
                    // Regenerate QR code if table number changed
                    if (existingTable.getNumber() != tableData.getNumber()) {
                        String newQrCode = qrCodeService.generateTableQRCode(tableData.getNumber());
                        existingTable.setQrCode(newQrCode);
                    }
                    
                    return tableRepository.save(existingTable);
                })
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + id));
    }
    
    public void deleteTable(Long id) {
        tableRepository.deleteById(id);
    }
    
    public boolean isTableNumberExists(int number) {
        return tableRepository.findByNumber(number).isPresent();
    }
    
    public boolean isTableNumberExistsExcludingId(int number, Long excludeId) {
        Optional<TableEntity> existing = tableRepository.findByNumber(number);
        return existing.isPresent() && !existing.get().getId().equals(excludeId);
    }
    
    public TableEntity updateTableStatus(Long id, String status) {
        return tableRepository.findById(id)
                .map(table -> {
                    table.setStatus(status);
                    return tableRepository.save(table);
                })
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + id));
    }
    
    public List<TableEntity> getAvailableTables() {
        return tableRepository.findByStatus(STATUS_AVAILABLE);
    }
    
    public List<TableEntity> getOccupiedTables() {
        return tableRepository.findByStatus(STATUS_OCCUPIED);
    }
    
    public String getTableQRCode(Long tableId) {
        TableEntity table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with id: " + tableId));
        
        // If QR code doesn't exist, generate it
        if (table.getQrCode() == null || table.getQrCode().isEmpty()) {
            String qrCode = qrCodeService.generateTableQRCode(table.getNumber());
            table.setQrCode(qrCode);
            tableRepository.save(table);
            return qrCode;
        }
        
        return table.getQrCode();
    }
    
    public void regenerateQRCode(Long tableId) {
        tableRepository.findById(tableId)
                .ifPresent(table -> {
                    String newQrCode = qrCodeService.generateTableQRCode(table.getNumber());
                    table.setQrCode(newQrCode);
                    tableRepository.save(table);
                });
    }
} 