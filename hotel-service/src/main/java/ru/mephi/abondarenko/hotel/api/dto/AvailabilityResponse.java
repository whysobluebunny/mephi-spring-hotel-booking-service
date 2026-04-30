package ru.mephi.abondarenko.hotel.api.dto;

public record AvailabilityResponse(
        boolean confirmed,
        String requestId,
        String bookingId,
        String message
) {
}
