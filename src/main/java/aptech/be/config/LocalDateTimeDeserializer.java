package aptech.be.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    
    private static final DateTimeFormatter[] FORMATTERS = {
        // ISO format với timezone
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"),
        
        // ISO format không có timezone
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
        
        // Standard format
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };
    
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getValueAsString();
        
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }
        
        // Trim whitespace
        dateString = dateString.trim();
        
        // Handle timezone conversion if needed
        if (dateString.endsWith("Z")) {
            // UTC time - convert to local timezone
            try {
                ZonedDateTime utcTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_INSTANT);
                return utcTime.toLocalDateTime();
            } catch (DateTimeParseException e) {
                // Fall back to other formatters
            }
        }
        
        // Try each formatter until one works
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                LocalDateTime parsed = LocalDateTime.parse(dateString, formatter);
                
                // If only date was provided, set time to end of day
                if (dateString.length() <= 10) {
                    parsed = parsed.withHour(23).withMinute(59).withSecond(59);
                }
                
                return parsed;
            } catch (DateTimeParseException e) {
                // Continue to next formatter
            }
        }
        
        // If none of the formatters work, throw an exception
        throw new IOException("Unable to parse date: " + dateString + 
            ". Expected formats: yyyy-MM-ddTHH:mm:ss, yyyy-MM-ddTHH:mm, yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, yyyy-MM-dd");
    }
} 