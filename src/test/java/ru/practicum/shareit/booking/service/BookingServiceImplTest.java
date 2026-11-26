package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.WrongStateException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long BOOKER_ID = 2L;
    private static final Long ITEM_ID = 3L;
    private static final Long BOOKING_ID = 4L;
    private static final Long NOT_OWNER_ID = 3L;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void shouldCreateBookingWithPastStartWhenItemIsAvailable() {
        User owner = user(OWNER_ID, "Owner");
        User booker = user(BOOKER_ID, "Booker");
        Item item = item(ITEM_ID, owner, true);
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        BookingRequestDto dto = new BookingRequestDto(ITEM_ID, start, end);

        when(userRepository.findById(BOOKER_ID)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(BOOKING_ID);
            return booking;
        });

        BookingDto result = bookingService.createBooking(BOOKER_ID, dto);

        assertThat(result.getId()).isEqualTo(BOOKING_ID);
        assertThat(result.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(result.getStart()).isEqualTo(start);
        assertThat(result.getEnd()).isEqualTo(end);
    }

    @Test
    void shouldReturn404WhenBookerNotFound() {
        BookingRequestDto dto = new BookingRequestDto(
                ITEM_ID, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(userRepository.findById(BOOKER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(BOOKER_ID, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn404WhenItemNotFound() {
        User booker = user(BOOKER_ID, "Booker");
        BookingRequestDto dto = new BookingRequestDto(
                ITEM_ID, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(userRepository.findById(BOOKER_ID)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(BOOKER_ID, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn404WhenOwnerBooksOwnItem() {
        User owner = user(OWNER_ID, "Owner");
        Item item = item(ITEM_ID, owner, true);
        BookingRequestDto dto = new BookingRequestDto(
                ITEM_ID, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.createBooking(OWNER_ID, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn400WhenItemIsUnavailable() {
        User owner = user(OWNER_ID, "Owner");
        User booker = user(BOOKER_ID, "Booker");
        Item item = item(ITEM_ID, owner, false);
        BookingRequestDto dto = new BookingRequestDto(
                ITEM_ID, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));
        when(userRepository.findById(BOOKER_ID)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.createBooking(BOOKER_ID, dto))
                .isInstanceOf(WrongStateException.class);
    }

    @Test
    void shouldReturn400WhenEndIsNotAfterStart() {
        User owner = user(OWNER_ID, "Owner");
        User booker = user(BOOKER_ID, "Booker");
        Item item = item(ITEM_ID, owner, true);
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        BookingRequestDto dto = new BookingRequestDto(ITEM_ID, start, start);
        when(userRepository.findById(BOOKER_ID)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> bookingService.createBooking(BOOKER_ID, dto))
                .isInstanceOf(WrongStateException.class);
    }

    @Test
    void shouldApproveBookingByOwner() {
        Booking booking = booking(BOOKING_ID, BookingStatus.WAITING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingDto result = bookingService.approveOrReject(BOOKING_ID, true, OWNER_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void shouldRejectBookingByOwner() {
        Booking booking = booking(BOOKING_ID, BookingStatus.WAITING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);

        BookingDto result = bookingService.approveOrReject(BOOKING_ID, false, OWNER_ID);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void shouldReturn403WhenApproveByNotOwner() {
        // пользователь НЕ владелец вещи — 403 раньше проверки статуса
        Booking booking = booking(BOOKING_ID, BookingStatus.WAITING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approveOrReject(BOOKING_ID, true, NOT_OWNER_ID))
                .isInstanceOf(ForbiddenException.class);
    }


    @Test
    void shouldReturn400WhenBookingIsAlreadyProcessed() {
        Booking booking = booking(BOOKING_ID, BookingStatus.APPROVED);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.approveOrReject(BOOKING_ID, true, OWNER_ID))
                .isInstanceOf(WrongStateException.class);
    }

    @Test
    void shouldReturn404WhenBookingIsRequestedByStranger() {
        Booking booking = booking(BOOKING_ID, BookingStatus.WAITING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingById(BOOKING_ID, 99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnBookingForBookerAndOwner() {
        Booking booking = booking(BOOKING_ID, BookingStatus.WAITING);
        when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

        assertThat(bookingService.getBookingById(BOOKING_ID, BOOKER_ID).getId()).isEqualTo(BOOKING_ID);
        assertThat(bookingService.getBookingById(BOOKING_ID, OWNER_ID).getId()).isEqualTo(BOOKING_ID);
    }

    @Test
    void shouldReturn404WhenUserForBookingsNotFound() {
        when(userRepository.existsById(BOOKER_ID)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.getBookingsByUser(BOOKER_ID, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn404WhenOwnerHasNoItems() {
        when(userRepository.existsById(OWNER_ID)).thenReturn(true);
        when(itemRepository.countByOwnerId(OWNER_ID)).thenReturn(0L);

        assertThatThrownBy(() -> bookingService.getBookingsByOwner(OWNER_ID, BookingState.ALL))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldSupportAllStatesForUser() {
        when(userRepository.existsById(BOOKER_ID)).thenReturn(true);
        when(bookingRepository.findByBookerIdOrderByStartDesc(BOOKER_ID)).thenReturn(List.of());
        when(bookingRepository.findCurrentByBooker_Id(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findPastByBooker_Id(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findFutureByBooker_Id(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(anyLong(), any(BookingStatus.class)))
                .thenReturn(List.of());

        for (BookingState state : BookingState.values()) {
            assertThat(bookingService.getBookingsByUser(BOOKER_ID, state)).isEmpty();
        }
    }

    @Test
    void shouldSupportAllStatesForOwner() {
        when(userRepository.existsById(OWNER_ID)).thenReturn(true);
        when(itemRepository.countByOwnerId(OWNER_ID)).thenReturn(1L);
        when(bookingRepository.findAllByOwner(OWNER_ID)).thenReturn(List.of());
        when(bookingRepository.findCurrentByOwner(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findPastByOwner(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findFutureByOwner(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());
        when(bookingRepository.findByOwnerAndStatus(anyLong(), any(BookingStatus.class))).thenReturn(List.of());

        for (BookingState state : BookingState.values()) {
            assertThat(bookingService.getBookingsByOwner(OWNER_ID, state)).isEmpty();
        }
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@mail.com");
        return user;
    }

    private Item item(Long id, User owner, boolean available) {
        Item item = new Item();
        item.setId(id);
        item.setOwner(owner);
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(available);
        return item;
    }

    private Booking booking(Long id, BookingStatus status) {
        User owner = user(OWNER_ID, "Owner");
        User booker = user(BOOKER_ID, "Booker");
        Item item = item(ITEM_ID, owner, true);
        Booking booking = new Booking();
        booking.setId(id);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(LocalDateTime.now().minusHours(2));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(status);
        return booking;
    }
}
