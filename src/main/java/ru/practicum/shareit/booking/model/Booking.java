package ru.practicum.shareit.booking.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private Long id;

    private Long itemId;
    private Long bookerId;

    private LocalDateTime start;
    private LocalDateTime end;

    private BookingStatus status;
}
