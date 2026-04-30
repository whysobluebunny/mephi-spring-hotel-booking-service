package ru.mephi.abondarenko.booking.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import ru.mephi.abondarenko.booking.domain.BookingStatus;

public record BookingResponse(
        String id,
        Long userId,
        Long roomId,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        String requestId,
        OffsetDateTime createdAt
) {
}
