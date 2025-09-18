package aptech.be.services.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Profile({"prod"})
public class CloudinaryStorageService implements StorageService {

    @Value("${cloudinary.cloud_name:}")
    private String cloudName;

    @Value("${cloudinary.api_key:}")
    private String apiKey;

    @Value("${cloudinary.api_secret:}")
    private String apiSecret;

    @Override
    public String uploadUserImage(MultipartFile file) throws Exception {
        // Sử dụng unsigned upload preset hoặc signed (đơn giản hoá bằng REST call)
        String preset = System.getenv().getOrDefault("CLOUDINARY_UPLOAD_PRESET", "");
        String endpoint = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        // Multipart/form-data manual (đơn giản hoá: dùng preset unsigned)
        var boundary = UUID.randomUUID().toString();
        var lineSep = "\r\n";
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (var out = conn.getOutputStream(); InputStream in = file.getInputStream()) {
            // upload_preset field
            out.write(("--" + boundary + lineSep).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"upload_preset\"" + lineSep + lineSep).getBytes(StandardCharsets.UTF_8));
            out.write((preset + lineSep).getBytes(StandardCharsets.UTF_8));

            // file field
            out.write(("--" + boundary + lineSep).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getOriginalFilename() + "\"" + lineSep).getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + (file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE) + lineSep + lineSep).getBytes(StandardCharsets.UTF_8));

            in.transferTo(out);
            out.write(lineSep.getBytes(StandardCharsets.UTF_8));

            // end
            out.write(("--" + boundary + "--" + lineSep).getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (var resp = conn.getInputStream()) {
                String body = new String(resp.readAllBytes(), StandardCharsets.UTF_8);
                // Rất đơn giản: tìm "secure_url":"..."
                int idx = body.indexOf("\"secure_url\":\"");
                if (idx > 0) {
                    int start = idx + 14;
                    int end = body.indexOf("\"", start);
                    if (end > start) {
                        return body.substring(start, end);
                    }
                }
            }
        }
        try (var err = conn.getErrorStream()) {
            String body = err != null ? new String(err.readAllBytes(), StandardCharsets.UTF_8) : "";
            throw new RuntimeException("Cloudinary upload failed: HTTP " + code + " - " + body);
        }
    }
}


