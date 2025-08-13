package aptech.be.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private final JwtFilterForAdmin jwtFilterForAdmin;
    private final JwtFilterForCustomer jwtFilterForCustomer;

    public SecurityConfig(
            @Qualifier("jwtFilterForAdmin") JwtFilterForAdmin jwtFilterForAdmin,
            @Qualifier("jwtFilterForCustomer") JwtFilterForCustomer jwtFilterForCustomer
    ) {
        this.jwtFilterForAdmin = jwtFilterForAdmin;
        this.jwtFilterForCustomer = jwtFilterForCustomer;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwtSecret}") String jwtSecret) {
        SecretKey secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> authorities = jwt.getClaimAsStringList("authorities");
            if (authorities == null) return Collections.emptyList();
            return authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        });
        return converter;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain payosPublicChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(request -> {
                String path = request.getServletPath();
                return path.startsWith("/api/orders/payment/qr/")
                    || path.equals("/api/orders/payment/payos/webhook")
                    || path.equals("/api/orders/payment/payos/return")
                    || path.equals("/api/orders/test-webhook")
                    || path.equals("/api/orders/manual-update-status")
                    || path.equals("/api/orders/payment/payos/cancel")
                    || path.equals("/api/orders/payos/test-create-url");
            })
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }


    @Bean
    @Order(1)
    public SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/login")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain staticResourceChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/uploads/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain dineInPublicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String path = request.getServletPath();
                    return path.startsWith("/api/tables/") 
                        || path.equals("/api/tables")
                        || path.equals("/api/categories")
                        || path.startsWith("/api/order/table/")
                        || path.matches("/api/dinein/table/\\d+")
                        || path.matches("/api/dinein/table/\\d+/current-order")
                        || path.matches("/api/dinein/table/\\d+/session")
                        || path.matches("/api/dinein/table/\\d+/call-staff")
                        || path.matches("/api/dinein/table/\\d+/request-payment")
                        || path.startsWith("/api/dinein/orders/create")
                        || path.equals("/api/dinein/order")
                        || path.matches("/api/dinein/table/\\d+/add-items")
                        || path.matches("/api/dinein/table/\\d+/all-orders")
                        || path.matches("/api/dinein/table/\\d+/summary")
                        || path.matches("/api/dinein/table/\\d+/debug")
                        || path.matches("/api/dinein/table/\\d+/end-session")
                        || path.startsWith("/api/dinein/points/")
                        || path.startsWith("/ws");
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(4)
    public SecurityFilterChain customerFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String path = request.getServletPath();
                    return path.startsWith("/api/customer/")|| path.startsWith("/api/orders/my");
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Các endpoint public
                        .requestMatchers(
                                "/api/customer/login",
                                "/api/customer/register",
                                "/api/customer/verify-code",
                                "/api/customer/google-login",
                                "/api/customer/forgot-password",
                                "/api/customer/reset-password"

                        ).permitAll()

                        // Các endpoint dành riêng cho customer
                        .requestMatchers(
                                "/api/customer/me/detail",
                                "/api/customer/change-password",
                                "/api/customer/addresses/**",
                                "/api/customer/vouchers/**",
                                "/api/orders/create",
                                "/api/orders/cancel/**",
                                "/api/orders/update/**",
                                "/api/orders/myorder",
                                "/api/orders/my/waiting-confirm",
                                "/api/orders/my/*/delivery-status",
                                "/api/orders/my/**",
                                "/api/orders/*/points-earned"
                        ).hasAuthority("ROLE_CUSTOMER")

                        // Nếu có cho customer xem detail đơn hàng riêng mình, giữ rule này (trong controller đã kiểm tra chủ đơn)
                        .requestMatchers("/api/orders/{id}").hasAuthority("ROLE_CUSTOMER")

                        // Các endpoint GET món ăn (menu, v.v...) cho phép public nếu muốn
                        .requestMatchers("/api/foods").permitAll()

                        // Còn lại bắt buộc phải đăng nhập
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilterForCustomer, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }



    @Bean
    @Order(5)
    public SecurityFilterChain adminStaffFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(request -> {
                    String path = request.getServletPath();
                    // Exclude /api/auth/login khỏi matcher này!
                    return (path.startsWith("/api/admin/")
                            || path.startsWith("/api/staff/")
                            || path.startsWith("/api/dinein/")
                            || (path.startsWith("/api/auth/") && !path.equals("/api/auth/login"))
                            || path.startsWith("/api/attendance/")
                            || path.startsWith("/api/notification/")
                            || path.startsWith("/attendance/reports/")
                            || path.startsWith("/api/orders/")
                    );
                })
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/users").hasAnyAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/attendance/reports/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/staff/request").hasAnyAuthority("ROLE_ADMIN","ROLE_STAFF")
                        .requestMatchers("/api/staff/me").hasAuthority("ROLE_STAFF")
                        .requestMatchers("/api/payment-methods/create").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/payment-methods/delete").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/payment-methods/update").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/payment-methods/get").permitAll()
                        .requestMatchers("/api/foods/create").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/foods/delete").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/foods/update").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/foods").permitAll()
                        .requestMatchers("/api/admin/vouchers/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/admin/vouchers/public").permitAll()
                        .requestMatchers("/api/admin/vouchers/customers").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/orders/waiting-confirm").hasAnyAuthority("ROLE_ADMIN","ROLE_STAFF")
                        .requestMatchers("/api/orders/{id}/delivery-status").hasAuthority("ROLE_STAFF")
                        .requestMatchers("/api/orders/{id}/admin-confirm").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/attendance/face-scan").permitAll()
                        
                        // DineIn endpoints for staff dashboard
                        .requestMatchers("/api/dinein/orders/all").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/orders/*/status").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/table/*/all-orders").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/table/*/summary").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/sessions/all").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/staff-calls/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/payment-requests/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/tables/*/bill").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/tables/*/confirm-payment").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/table/*/end-session").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/dinein/debug/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_STAFF")
                        .requestMatchers("/api/admin/dashboard/**").hasAuthority("ROLE_ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilterForAdmin, UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }
    @Bean
    @Order(6)
    public SecurityFilterChain shipperFilterChain(HttpSecurity http, JwtFilterForShipper jwtFilterForShipper) throws Exception {
        http
                .securityMatcher("/api/shipper/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("SHIPPER")
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .addFilterBefore(jwtFilterForShipper, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
