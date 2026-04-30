package ru.mephi.abondarenko.hotel.api.dto;

public record RoomOccupancyStatsResponse(
        Long roomId,
        Long hotelId,
        String hotelName,
        String roomNumber,
        boolean operationallyAvailable,
        long timesBooked,
        long activeLocks
) {
}
