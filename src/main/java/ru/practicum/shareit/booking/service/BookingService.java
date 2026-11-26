package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import java.util.List;

public interface BookingService {
    BookingDto createBooking(Long bookerId, BookingRequestDto dto);

    BookingDto getBookingById(Long bookingId, Long userId);

    BookingDto approveOrReject(Long bookingId, boolean approved, Long ownerId);

    List<BookingDto> getBookingsByUser(Long userId, BookingState state);

    List<BookingDto> getBookingsByOwner(Long ownerId, BookingState state);
}

