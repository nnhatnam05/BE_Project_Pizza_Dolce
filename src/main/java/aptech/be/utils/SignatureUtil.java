package aptech.be.utils;

import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;

import java.util.*;

public class SignatureUtil {
    public static boolean isValidSignature(String jsonData, String signature, String checksumKey) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            // Loại bỏ trường signature nếu có
            jsonObject.remove("signature");

            // Sắp xếp key theo alphabet
            List<String> keys = new ArrayList<>();
            jsonObject.keys().forEachRemaining(keys::add);
            Collections.sort(keys);

            // Ghép thành chuỗi key1=value1&key2=value2...
            StringBuilder dataToSign = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String value = jsonObject.get(key).toString();
                dataToSign.append(key).append("=").append(value);
                if (i < keys.size() - 1) dataToSign.append("&");
            }

            // Tạo signature
            String calculatedSignature = new HmacUtils("HmacSHA256", checksumKey)
                    .hmacHex(dataToSign.toString());

            boolean match = calculatedSignature.equals(signature);
            if (!match) {
                System.err.println("[SignatureUtil] Signature không khớp!\n  Data: " + dataToSign + "\n  Provided: " + signature + "\n  Calculated: " + calculatedSignature);
            }
            return match;
        } catch (Exception e) {
            System.err.println("[SignatureUtil] Lỗi khi xác thực signature: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}