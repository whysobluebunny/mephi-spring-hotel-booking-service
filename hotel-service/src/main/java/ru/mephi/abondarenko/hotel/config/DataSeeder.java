package ru.mephi.abondarenko.hotel.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mephi.abondarenko.hotel.api.dto.CreateHotelRequest;
import ru.mephi.abondarenko.hotel.api.dto.CreateRoomRequest;
import ru.mephi.abondarenko.hotel.service.HotelService;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final HotelService hotelService;

    @Bean
    CommandLineRunner hotelSeedRunner() {
        return args -> {
            if (!hotelService.getHotels().isEmpty()) {
                return;
            }
            var hotel = hotelService.createHotel(new CreateHotelRequest("Aurora", "Moscow, Tverskaya 10"));
            hotelService.createRoom(new CreateRoomRequest(hotel.getId(), "101", true));
            hotelService.createRoom(new CreateRoomRequest(hotel.getId(), "102", true));
            hotelService.createRoom(new CreateRoomRequest(hotel.getId(), "103", true));
        };
    }
}
