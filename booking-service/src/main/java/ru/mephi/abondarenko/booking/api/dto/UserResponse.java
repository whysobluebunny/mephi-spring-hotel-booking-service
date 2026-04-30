package ru.mephi.abondarenko.booking.api.dto;

import ru.mephi.abondarenko.booking.domain.Role;

public record UserResponse(
        Long id,
        String username,
        Role role
) {
}
