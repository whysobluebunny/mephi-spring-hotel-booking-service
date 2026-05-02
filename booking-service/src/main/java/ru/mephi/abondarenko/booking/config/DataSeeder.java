package ru.mephi.abondarenko.booking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mephi.abondarenko.booking.domain.Role;
import ru.mephi.abondarenko.booking.domain.UserEntity;
import ru.mephi.abondarenko.booking.repository.UserRepository;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner userSeedRunner() {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                UserEntity admin = new UserEntity();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
            }

            if (userRepository.findByUsername("user").isEmpty()) {
                UserEntity user = new UserEntity();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user"));
                user.setRole(Role.USER);
                userRepository.save(user);
            }
        };
    }
}
