package ru.practicum.shareit.booking.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookingRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now().withNano(0);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(user("Owner", "owner@mail.com"));
        booker = userRepository.save(user("Booker", "booker@mail.com"));
        item = itemRepository.save(item(owner, "Item"));
    }

    @Test
    void shouldOrderBookerBookingsByStartDesc() {
        Booking oldBooking = booking(item, booker, NOW.minusDays(3), NOW.minusDays(2), BookingStatus.APPROVED);
        Booking newBooking = booking(item, booker, NOW.minusDays(1), NOW, BookingStatus.APPROVED);
        bookingRepository.saveAll(List.of(oldBooking, newBooking));

        List<Booking> result = bookingRepository.findByBookerIdOrderByStartDesc(booker.getId());

        assertThat(result).extracting(Booking::getId)
                .containsExactly(newBooking.getId(), oldBooking.getId());
    }

    @Test
    void shouldFindCurrentBookerBooking() {
        Booking past = booking(item, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        Booking current = booking(item, booker, NOW.minusHours(1), NOW.plusHours(1), BookingStatus.APPROVED);
        Booking future = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        bookingRepository.saveAll(List.of(past, current, future));

        List<Booking> result = bookingRepository.findCurrentByBooker_Id(booker.getId(), NOW);

        assertThat(result).extracting(Booking::getId).containsExactly(current.getId());
    }

    @Test
    void shouldOrderPastBookerBookingsByEndDesc() {
        Booking older = booking(item, booker, NOW.minusDays(3), NOW.minusDays(2), BookingStatus.APPROVED);
        Booking newer = booking(item, booker, NOW.minusDays(2), NOW.minusHours(1), BookingStatus.APPROVED);
        bookingRepository.saveAll(List.of(older, newer));

        List<Booking> result = bookingRepository.findPastByBooker_Id(booker.getId(), NOW);

        assertThat(result).extracting(Booking::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void shouldOrderFutureBookerBookingsByStartDesc() {
        Booking near = booking(item, booker, NOW.plusHours(1), NOW.plusHours(2), BookingStatus.APPROVED);
        Booking far = booking(item, booker, NOW.plusDays(2), NOW.plusDays(3), BookingStatus.APPROVED);
        bookingRepository.saveAll(List.of(near, far));

        List<Booking> result = bookingRepository.findFutureByBooker_Id(booker.getId(), NOW);

        assertThat(result).extracting(Booking::getId)
                .containsExactly(far.getId(), near.getId());
    }

    @Test
    void shouldFindBookerBookingsByStatus() {
        Booking waiting = booking(item, booker, NOW.plusHours(1), NOW.plusHours(2), BookingStatus.WAITING);
        Booking rejected = booking(item, booker, NOW.plusHours(3), NOW.plusHours(4), BookingStatus.REJECTED);
        bookingRepository.saveAll(List.of(waiting, rejected));

        assertThat(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(
                booker.getId(), BookingStatus.WAITING))
                .extracting(Booking::getId)
                .containsExactly(waiting.getId());

        assertThat(bookingRepository.findByBookerIdAndStatusOrderByStartDesc(
                booker.getId(), BookingStatus.REJECTED))
                .extracting(Booking::getId)
                .containsExactly(rejected.getId());
    }

    @Test
    void shouldFindOwnerBookings() {
        Booking booking = booking(item, booker, NOW.minusHours(1), NOW.plusHours(1), BookingStatus.APPROVED);
        bookingRepository.save(booking);

        assertThat(bookingRepository.findAllByOwner(owner.getId()))
                .extracting(Booking::getId)
                .containsExactly(booking.getId());
        assertThat(bookingRepository.findCurrentByOwner(owner.getId(), NOW))
                .extracting(Booking::getId)
                .containsExactly(booking.getId());
    }

    @Test
    void shouldFindOwnerPastAndFutureBookings() {
        Booking past = booking(item, booker, NOW.minusDays(3), NOW.minusDays(2), BookingStatus.APPROVED);
        Booking future = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        bookingRepository.saveAll(List.of(past, future));

        assertThat(bookingRepository.findPastByOwner(owner.getId(), NOW))
                .extracting(Booking::getId)
                .containsExactly(past.getId());
        assertThat(bookingRepository.findFutureByOwner(owner.getId(), NOW))
                .extracting(Booking::getId)
                .containsExactly(future.getId());
    }

    @Test
    void shouldFindOwnerBookingsByStatus() {
        Booking waiting = booking(item, booker, NOW.plusHours(1), NOW.plusHours(2), BookingStatus.WAITING);
        bookingRepository.save(waiting);

        assertThat(bookingRepository.findByOwnerAndStatus(owner.getId(), BookingStatus.WAITING))
                .extracting(Booking::getId)
                .containsExactly(waiting.getId());
    }

    @Test
    void shouldFindBookingsByItemIdsExcludingRejectedWithFetchedItem() {
        Booking approved = booking(item, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.APPROVED);
        Booking waiting = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.WAITING);
        Booking rejected = booking(item, booker, NOW.plusDays(3), NOW.plusDays(4), BookingStatus.REJECTED);
        bookingRepository.saveAll(List.of(approved, waiting, rejected));
        entityManager.flush();
        entityManager.clear();

        List<Booking> result = bookingRepository.findByItemIdInAndStatusNot(
                List.of(item.getId()), BookingStatus.REJECTED);

        assertThat(result).extracting(Booking::getId)
                .containsExactlyInAnyOrder(approved.getId(), waiting.getId());
        assertThat(result).allSatisfy(booking -> assertThat(booking.getItem().getName()).isEqualTo("Item"));
    }

    @Test
    void shouldCheckCompletedBookingRegardlessOfStatus() {
        Booking waitingPast = booking(item, booker, NOW.minusDays(2), NOW.minusDays(1), BookingStatus.WAITING);
        bookingRepository.save(waitingPast);

        assertThat(bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                item.getId(), booker.getId(), NOW)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenBookingHasNotFinishedYet() {
        Booking future = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        bookingRepository.save(future);

        assertThat(bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                item.getId(), booker.getId(), NOW)).isFalse();
    }

    @Test
    void shouldFindBookingsByItemId() {
        Booking booking = booking(item, booker, NOW.plusDays(1), NOW.plusDays(2), BookingStatus.APPROVED);
        bookingRepository.save(booking);

        assertThat(bookingRepository.findByItemId(item.getId()))
                .extracting(Booking::getId)
                .containsExactly(booking.getId());
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Item item(User itemOwner, String name) {
        Item item = new Item();
        item.setOwner(itemOwner);
        item.setName(name);
        item.setDescription("Description");
        item.setAvailable(true);
        return item;
    }

    private Booking booking(Item bookingItem, User bookingBooker,
                            LocalDateTime start, LocalDateTime end, BookingStatus status) {
        Booking booking = new Booking();
        booking.setItem(bookingItem);
        booking.setBooker(bookingBooker);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(status);
        return booking;
    }
}
