package ru.practicum.shareit.booking.mapper;

import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;

public class BookingMapper {

    public static BookingShortDto toShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }
        return new BookingShortDto(
                booking.getId(),
                booking.getStart(),
                booking.getEnd(),
                booking.getStatus()
        );
    }
}
