package ru.practicum.shareit.mapper;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.mapper.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MapperTest {

    private static final Long ID = 1L;

    @Test
    void shouldReturnNullWhenBookingIsNull() {
        assertThat(BookingMapper.toShortDto(null)).isNull();
    }

    @Test
    void shouldMapBookingToShortDto() {
        Booking booking = new Booking();
        booking.setId(ID);
        booking.setStart(LocalDateTime.now().minusHours(2));
        booking.setEnd(LocalDateTime.now().minusHours(1));
        booking.setStatus(BookingStatus.APPROVED);

        BookingShortDto dto = BookingMapper.toShortDto(booking);

        assertThat(dto.getId()).isEqualTo(ID);
        assertThat(dto.getStart()).isEqualTo(booking.getStart());
        assertThat(dto.getEnd()).isEqualTo(booking.getEnd());
        assertThat(dto.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void shouldReturnNullWhenCommentIsNull() {
        assertThat(CommentMapper.toDto(null)).isNull();
    }

    @Test
    void shouldMapCommentWithAuthorName() {
        User author = new User();
        author.setName("Alice");
        Comment comment = new Comment();
        comment.setId(ID);
        comment.setText("Text");
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now().minusHours(1));

        CommentDto dto = CommentMapper.toDto(comment);

        assertThat(dto.getId()).isEqualTo(ID);
        assertThat(dto.getText()).isEqualTo("Text");
        assertThat(dto.getAuthorName()).isEqualTo("Alice");
        assertThat(dto.getCreated()).isEqualTo(comment.getCreated());
    }
}
