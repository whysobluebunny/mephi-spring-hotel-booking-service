package ru.mephi.abondarenko.booking.api.dto;

public record HotelRoomResponse(
        Long id,
        Long hotelId,
        String hotelName,
        String number,
        boolean available,
        long timesBooked
) {
}
