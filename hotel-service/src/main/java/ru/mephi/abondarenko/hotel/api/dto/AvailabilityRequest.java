package ru.mephi.abondarenko.hotel.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityRequest(
        @NotBlank String bookingId,
        @NotBlank String requestId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
