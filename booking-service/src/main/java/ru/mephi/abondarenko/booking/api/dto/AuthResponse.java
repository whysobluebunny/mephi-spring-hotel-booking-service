package ru.mephi.abondarenko.booking.api.dto;

import ru.mephi.abondarenko.booking.domain.Role;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        Role role
) {
}
