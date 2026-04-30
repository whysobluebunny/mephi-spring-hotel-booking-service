package ru.mephi.abondarenko.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import ru.mephi.abondarenko.booking.domain.Role;

public record UpdateUserRequest(
        @NotNull Long id,
        String username,
        String password,
        Role role
) {
}
