package ru.mephi.abondarenko.hotel.api.dto;

public record HotelAnalyticsResponse(
        Long hotelId,
        String hotelName,
        long totalRooms,
        long operationallyAvailableRooms,
        long activeLocks,
        long totalTimesBooked,
        String leastBookedRoomNumber,
        String mostBookedRoomNumber
) {
}
