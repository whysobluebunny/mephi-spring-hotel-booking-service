package ru.mephi.abondarenko.booking.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mephi.abondarenko.booking.api.dto.AuthRequest;
import ru.mephi.abondarenko.booking.api.dto.AuthResponse;
import ru.mephi.abondarenko.booking.api.dto.CreateUserRequest;
import ru.mephi.abondarenko.booking.api.dto.RegisterRequest;
import ru.mephi.abondarenko.booking.api.dto.UpdateUserRequest;
import ru.mephi.abondarenko.booking.api.dto.UserResponse;
import ru.mephi.abondarenko.booking.api.mapper.BookingMapper;
import ru.mephi.abondarenko.booking.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final BookingMapper bookingMapper;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/auth")
    public AuthResponse auth(@Valid @RequestBody AuthRequest request) {
        return userService.authenticate(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return bookingMapper.toResponse(userService.create(request));
    }

    @PatchMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@Valid @RequestBody UpdateUserRequest request) {
        return bookingMapper.toResponse(userService.update(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
