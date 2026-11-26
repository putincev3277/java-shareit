package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.WrongStateException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.dto.UserShortDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public BookingDto createBooking(Long bookerId, BookingRequestDto dto) {
        User booker = userRepository.findById(bookerId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + bookerId + " не найден"));

        Item item = itemRepository.findById(dto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с ID " + dto.getItemId() + " не найдена"));

        if (item.getOwner().getId().equals(bookerId)) {
            throw new NotFoundException("Владелец не может забронировать свою вещь");
        }

        if (!item.isAvailable()) {
            throw new WrongStateException("Вещь сейчас недоступна для бронирования");
        }

        if (dto.getStart() == null || dto.getEnd() == null) {
            throw new WrongStateException("Даты бронирования обязательны");
        }
        if (!dto.getEnd().isAfter(dto.getStart())) {
            throw new WrongStateException("Дата окончания бронирования должна быть после даты начала");
        }

        // Прошедший start не проверяем: по ТЗ такое бронирование валидно,
        // а проверка now + 1 секунда приводила бы к гонке в тестах.
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(dto.getStart());
        booking.setEnd(dto.getEnd());
        booking.setStatus(BookingStatus.WAITING);

        Booking saved = bookingRepository.save(booking);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с ID " + bookingId + " не найдено"));

        long itemOwnerId = booking.getItem().getOwner().getId();
        long bookerId = booking.getBooker().getId();

        if (!Objects.equals(userId, itemOwnerId) && !Objects.equals(userId, bookerId)) {
            throw new NotFoundException("Бронирование с ID " + bookingId + " не найдено");
        }

        return toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto approveOrReject(Long bookingId, boolean approved, Long ownerId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с ID " + bookingId + " не найдено"));

        if (!Objects.equals(booking.getItem().getOwner().getId(), ownerId)) {
            throw new ForbiddenException("Подтвердить или отклонить бронирование может только владелец вещи");
        }

        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new WrongStateException("Статус бронирования уже изменён");
        }

        if (approved) {
            booking.setStatus(BookingStatus.APPROVED);
        } else {
            booking.setStatus(BookingStatus.REJECTED);
        }

        Booking updated = bookingRepository.save(booking);
        return toDto(updated);
    }

    @Override
    public List<BookingDto> getBookingsByUser(Long userId, BookingState state) {
        ensureUserExists(userId);
        BookingState effectiveState = state == null ? BookingState.ALL : state;
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (effectiveState) {
            case ALL:
                bookings = bookingRepository.findByBookerIdOrderByStartDesc(userId);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByBooker_Id(userId, now);
                break;
            case PAST:
                bookings = bookingRepository.findPastByBooker_Id(userId, now);
                break;
            case FUTURE:
                bookings = bookingRepository.findFutureByBooker_Id(userId, now);
                break;
            case WAITING:
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.WAITING);
                break;
            case REJECTED:
                bookings = bookingRepository.findByBookerIdAndStatusOrderByStartDesc(userId, BookingStatus.REJECTED);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный статус поиска: " + effectiveState);
        }
        return bookings.stream().map(this::toDto).toList();
    }

    @Override
    public List<BookingDto> getBookingsByOwner(Long ownerId, BookingState state) {
        ensureUserExists(ownerId);

        if (itemRepository.countByOwnerId(ownerId) == 0) {
            throw new NotFoundException("Пользователь с ID " + ownerId + " не является владельцем ни одной вещи");
        }

        BookingState effectiveState = state == null ? BookingState.ALL : state;
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings;

        switch (effectiveState) {
            case ALL:
                bookings = bookingRepository.findAllByOwner(ownerId);
                break;
            case CURRENT:
                bookings = bookingRepository.findCurrentByOwner(ownerId, now);
                break;
            case PAST:
                bookings = bookingRepository.findPastByOwner(ownerId, now);
                break;
            case FUTURE:
                bookings = bookingRepository.findFutureByOwner(ownerId, now);
                break;
            case WAITING:
                bookings = bookingRepository.findByOwnerAndStatus(ownerId, BookingStatus.WAITING);
                break;
            case REJECTED:
                bookings = bookingRepository.findByOwnerAndStatus(ownerId, BookingStatus.REJECTED);
                break;
            default:
                throw new IllegalArgumentException("Неизвестный статус поиска: " + effectiveState);
        }

        return bookings.stream().map(this::toDto).toList();
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }

    private BookingDto toDto(Booking booking) {
        Item item = booking.getItem();
        User booker = booking.getBooker();

        ItemShortDto itemDto = new ItemShortDto(item.getId(), item.getName());
        UserShortDto bookerDto = new UserShortDto(booker.getId(), booker.getName());

        return BookingDto.builder()
                .id(booking.getId())
                .item(itemDto)
                .booker(bookerDto)
                .start(booking.getStart())
                .end(booking.getEnd())
                .status(booking.getStatus())
                .build();
    }
}
