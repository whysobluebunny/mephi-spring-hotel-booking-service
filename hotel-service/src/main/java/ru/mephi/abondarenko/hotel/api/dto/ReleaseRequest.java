package ru.mephi.abondarenko.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReleaseRequest(@NotBlank String requestId) {
}
