package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ITEM_ID = 3L;
    private static final Long BOOKING_ID = 4L;
    private static final Long COMMENT_ID = 5L;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Test
    void shouldReturn404WhenAddingItemForMissingOwner() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addItem(OWNER_ID, new ItemCreateDto("Drill", "Desc", true)))
                .isInstanceOf(NotFoundException.class);

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void shouldAddItemForOwner() {
        User owner = user(OWNER_ID, "Owner");
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(ITEM_ID);
            return item;
        });

        ItemDto result = itemService.addItem(OWNER_ID, new ItemCreateDto("Drill", "Desc", true));

        assertThat(result.getId()).isEqualTo(ITEM_ID);
        assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void shouldReturn404WhenUpdatingMissingItem() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("New name");

        assertThatThrownBy(() -> itemService.updateItem(ITEM_ID, OWNER_ID, dto))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn404WhenGettingMissingItemDetails() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getItemWithDetails(ITEM_ID, OWNER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturn403WhenItemIsUpdatedByNotOwner() {
        Item item = item(OWNER_ID, true);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("New name");

        assertThatThrownBy(() -> itemService.updateItem(ITEM_ID, OTHER_USER_ID, dto))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
        Item item = item(OWNER_ID, true);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        ItemUpdateDto dto = new ItemUpdateDto();
        dto.setName("New name");

        ItemDto result = itemService.updateItem(ITEM_ID, OWNER_ID, dto);

        assertThat(result.getName()).isEqualTo("New name");
        assertThat(result.getDescription()).isEqualTo("Description");
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void shouldReturnEmptyListWhenSearchTextIsBlank() {
        assertThat(itemService.searchItems(" ")).isEmpty();
    }

    @Test
    void shouldSearchItemsByText() {
        Item item = item(OWNER_ID, true);
        when(itemRepository.search("drill")).thenReturn(List.of(item));

        List<ItemDto> result = itemService.searchItems("drill");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Item");
    }

    @Test
    void shouldShowBookingsToOwnerAndCommentsToEveryone() {
        Item item = item(OWNER_ID, true);
        Booking last = booking(BOOKING_ID, BookingStatus.WAITING,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        Booking next = booking(BOOKING_ID + 1, BookingStatus.APPROVED,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusHours(1));
        Comment comment = comment(COMMENT_ID, item);

        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemId(ITEM_ID)).thenReturn(List.of(last, next));
        when(commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(ITEM_ID))
                .thenReturn(List.of(comment));

        ItemWithBookingsDto result = itemService.getItemWithDetails(ITEM_ID, OWNER_ID);

        assertThat(result.getLastBooking()).isNotNull();
        assertThat(result.getLastBooking().getId()).isEqualTo(BOOKING_ID);
        assertThat(result.getNextBooking()).isNotNull();
        assertThat(result.getNextBooking().getId()).isEqualTo(BOOKING_ID + 1);
        assertThat(result.getComments()).hasSize(1);
    }

    @Test
    void shouldHideBookingsFromNotOwnerButShowComments() {
        Item item = item(OWNER_ID, true);
        Booking last = booking(BOOKING_ID, BookingStatus.APPROVED,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        Comment comment = comment(COMMENT_ID, item);

        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemId(ITEM_ID)).thenReturn(List.of(last));
        when(commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(ITEM_ID))
                .thenReturn(List.of(comment));

        ItemWithBookingsDto result = itemService.getItemWithDetails(ITEM_ID, OTHER_USER_ID);

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNull();
        assertThat(result.getComments()).hasSize(1);
    }

    @Test
    void shouldChooseLastBookingAsMaxEndAndNextAsMinStart() {
        Item item = item(OWNER_ID, true);
        Booking rejected = booking(10L, BookingStatus.REJECTED,
                LocalDateTime.now().minusHours(5), LocalDateTime.now().minusMinutes(10));
        Booking approvedPast = booking(11L, BookingStatus.APPROVED,
                LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(3));
        Booking waitingPast = booking(12L, BookingStatus.WAITING,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        Booking current = booking(13L, BookingStatus.APPROVED,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        Booking waitingFuture = booking(14L, BookingStatus.WAITING,
                LocalDateTime.now().plusMinutes(10), LocalDateTime.now().plusMinutes(20));
        Booking approvedFutureLater = booking(15L, BookingStatus.APPROVED,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3));
        Booking approvedFutureEarlier = booking(16L, BookingStatus.APPROVED,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusHours(1));

        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemId(ITEM_ID)).thenReturn(List.of(
                rejected, approvedPast, waitingPast, current,
                waitingFuture, approvedFutureLater, approvedFutureEarlier));
        when(commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(ITEM_ID)).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getItemWithDetails(ITEM_ID, OWNER_ID);

        assertThat(result.getLastBooking().getId()).isEqualTo(waitingPast.getId());
        assertThat(result.getNextBooking().getId()).isEqualTo(approvedFutureEarlier.getId());
    }

    @Test
    void shouldNotIncludeFutureBookingInLastBooking() {
        Item item = item(OWNER_ID, true);
        Booking future = booking(BOOKING_ID, BookingStatus.APPROVED,
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2));

        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemId(ITEM_ID)).thenReturn(List.of(future));
        when(commentRepository.findByItemIdWithAuthorOrderByCreatedDesc(ITEM_ID)).thenReturn(List.of());

        ItemWithBookingsDto result = itemService.getItemWithDetails(ITEM_ID, OWNER_ID);

        assertThat(result.getLastBooking()).isNull();
        assertThat(result.getNextBooking()).isNotNull();
        assertThat(result.getNextBooking().getId()).isEqualTo(BOOKING_ID);
    }

    @Test
    void shouldFillBookingsAndCommentsInOwnerItemsList() {
        Item item = item(OWNER_ID, true);
        Booking last = booking(BOOKING_ID, BookingStatus.APPROVED,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        Comment comment = comment(COMMENT_ID, item);

        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, "Owner")));
        when(itemRepository.findByOwnerIdOrderByIdAsc(OWNER_ID)).thenReturn(List.of(item));
        when(bookingRepository.findByItemIdInAndStatusNot(List.of(ITEM_ID), BookingStatus.REJECTED))
                .thenReturn(List.of(last));
        when(commentRepository.findByItemIdInWithAuthorOrderByCreatedDesc(List.of(ITEM_ID)))
                .thenReturn(List.of(comment));

        List<ItemDto> result = itemService.getOwnerItemsWithBookingDates(OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLastBooking()).isNotNull();
        assertThat(result.get(0).getLastBooking().getId()).isEqualTo(BOOKING_ID);
        assertThat(result.get(0).getNextBooking()).isNull();
        assertThat(result.get(0).getComments()).hasSize(1);
    }

    @Test
    void shouldAddCommentWhenUserHasCompletedBooking() {
        Item item = item(OWNER_ID, true);
        User author = user(OTHER_USER_ID, "Booker");
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(author));
        when(bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                any(), any(), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(COMMENT_ID);
            return saved;
        });

        CommentDto result = itemService.addComment(ITEM_ID, OTHER_USER_ID, new CommentCreateDto("Good item"));

        assertThat(result.getId()).isEqualTo(COMMENT_ID);
        assertThat(result.getText()).isEqualTo("Good item");
        assertThat(result.getAuthorName()).isEqualTo("Booker");
    }

    @Test
    void shouldReturn400WhenUserHasNoCompletedBooking() {
        Item item = item(OWNER_ID, true);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
        when(userRepository.findById(OTHER_USER_ID)).thenReturn(Optional.of(user(OTHER_USER_ID, "Booker")));
        when(bookingRepository.existsByItemIdAndBookerIdAndEndBefore(
                any(), any(), any(LocalDateTime.class))).thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(
                ITEM_ID, OTHER_USER_ID, new CommentCreateDto("Good item")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commentRepository, never()).save(any(Comment.class));
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@mail.com");
        return user;
    }

    private Item item(Long ownerId, boolean available) {
        Item item = new Item();
        item.setId(ITEM_ID);
        item.setOwner(user(ownerId, "Owner"));
        item.setName("Item");
        item.setDescription("Description");
        item.setAvailable(available);
        return item;
    }

    private Booking booking(Long id, BookingStatus status, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setItem(item(OWNER_ID, true));
        booking.setBooker(user(OTHER_USER_ID, "Booker"));
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(status);
        return booking;
    }

    private Comment comment(Long id, Item item) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setItem(item);
        comment.setAuthor(user(OTHER_USER_ID, "Booker"));
        comment.setText("Text");
        comment.setCreated(LocalDateTime.now().minusMinutes(5));
        return comment;
    }
}
