package ru.mephi.abondarenko.booking.api.dto;

import java.time.LocalDate;

public record HotelAvailabilityRequest(
        String bookingId,
        String requestId,
        LocalDate startDate,
        LocalDate endDate
) {
}
