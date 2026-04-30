package ru.mephi.abondarenko.booking.api.mapper;

import org.mapstruct.Mapper;
import ru.mephi.abondarenko.booking.api.dto.BookingResponse;
import ru.mephi.abondarenko.booking.api.dto.UserResponse;
import ru.mephi.abondarenko.booking.domain.Booking;
import ru.mephi.abondarenko.booking.domain.UserEntity;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingResponse toResponse(Booking booking);

    UserResponse toResponse(UserEntity user);
}
