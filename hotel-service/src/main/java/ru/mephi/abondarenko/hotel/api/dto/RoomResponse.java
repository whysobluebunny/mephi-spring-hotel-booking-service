package ru.mephi.abondarenko.hotel.api.dto;

public record RoomResponse(
        Long id,
        Long hotelId,
        String hotelName,
        String number,
        boolean available,
        long timesBooked
) {
}
