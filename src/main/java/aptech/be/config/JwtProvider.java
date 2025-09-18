package aptech.be.config;

import org.springframework.stereotype.Component;

@Component
@Deprecated
public class JwtProvider {
    // Đã thay thế bởi JwtService. Lưu bean rỗng để tránh lỗi autowire tạm thời.
}
