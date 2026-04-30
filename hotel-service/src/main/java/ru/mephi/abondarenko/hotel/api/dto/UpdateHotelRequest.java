package ru.mephi.abondarenko.hotel.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateHotelRequest(
        @NotNull Long id,
        String name,
        String address
) {
}
