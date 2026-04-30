package ru.mephi.abondarenko.booking.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateBookingRequest(
        Long roomId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        boolean autoSelect,
        Long hotelId
) {
}
