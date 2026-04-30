package ru.mephi.abondarenko.booking.api.dto;

public record HotelAvailabilityResponse(
        boolean confirmed,
        String requestId,
        String bookingId,
        String message
) {
}
