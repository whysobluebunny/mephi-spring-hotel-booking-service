package ru.mephi.abondarenko.hotel.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.mephi.abondarenko.hotel.api.dto.HotelResponse;
import ru.mephi.abondarenko.hotel.api.dto.RoomResponse;
import ru.mephi.abondarenko.hotel.domain.Hotel;
import ru.mephi.abondarenko.hotel.domain.Room;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    HotelResponse toResponse(Hotel hotel);

    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomResponse toResponse(Room room);
}
