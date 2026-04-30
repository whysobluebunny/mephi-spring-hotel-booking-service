package ru.mephi.abondarenko.hotel.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRoomRequest(
        @NotNull Long id,
        Long hotelId,
        String number,
        Boolean available
) {
}
