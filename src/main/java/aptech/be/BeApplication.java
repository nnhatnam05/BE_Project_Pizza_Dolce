package aptech.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import aptech.be.repositories.UserRepository;
import aptech.be.models.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class BeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeApplication.class, args);
	}

	private static final Logger log = LoggerFactory.getLogger(BeApplication.class);

	@Bean
	public CommandLineRunner ensureDefaultAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			final String username = "admin";
			userRepository.findByUsername(username).ifPresentOrElse(
				existing -> {
					log.info("Default admin user '{}' already exists (id={}). Skipping creation.", username, existing.getId());
				},
				() -> {
					UserEntity admin = new UserEntity();
					admin.setUsername(username);
					admin.setPassword(passwordEncoder.encode("123"));
					admin.setName("Administrator");
					admin.setRole("ADMIN");
					admin.setIsActive(true);
					userRepository.save(admin);
					log.info("Default admin user '{}' created successfully.", username);
				}
			);
		};
	}
}
