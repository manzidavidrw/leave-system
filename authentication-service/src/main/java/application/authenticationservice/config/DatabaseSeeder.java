package application.authenticationservice.config;

import application.authenticationservice.Enum.Role;
import application.authenticationservice.entity.User;
import application.authenticationservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "admin@system.com";

        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode("Admin@123")); // Change this password!
            admin.setFirstName("System");
            admin.setLastName("Administrator");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setTwoFaEnabled(false);

            userRepository.save(admin);

            log.info("Admin user seeded successfully");
            log.info("Email: {}", adminEmail);
            log.info("Password: Admin@123 (PLEASE CHANGE THIS!)");
        } else {
            log.info("Admin user already exists");
        }
    }
}