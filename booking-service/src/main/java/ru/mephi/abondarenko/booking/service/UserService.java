package ru.mephi.abondarenko.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.abondarenko.booking.api.dto.AuthRequest;
import ru.mephi.abondarenko.booking.api.dto.AuthResponse;
import ru.mephi.abondarenko.booking.api.dto.CreateUserRequest;
import ru.mephi.abondarenko.booking.api.dto.RegisterRequest;
import ru.mephi.abondarenko.booking.api.dto.UpdateUserRequest;
import ru.mephi.abondarenko.booking.domain.Role;
import ru.mephi.abondarenko.booking.domain.UserEntity;
import ru.mephi.abondarenko.booking.repository.UserRepository;
import ru.mephi.abondarenko.booking.security.JwtService;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        UserEntity saved = userRepository.save(user);
        return new AuthResponse(jwtService.generateUserToken(saved), saved.getId(), saved.getUsername(), saved.getRole());
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest request) {
        UserEntity user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return new AuthResponse(jwtService.generateUserToken(user), user.getId(), user.getUsername(), user.getRole());
    }

    @Transactional
    public UserEntity create(CreateUserRequest request) {
        UserEntity user = new UserEntity();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        return userRepository.save(user);
    }

    @Transactional
    public UserEntity update(UpdateUserRequest request) {
        UserEntity user = userRepository.findById(request.id())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
