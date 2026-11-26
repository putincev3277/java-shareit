package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public class ItemMapper {

    public static ItemDto toDto(Item item) {
        return toDto(item, List.of(), null, null);
    }

    public static ItemDto toDto(Item item, List<CommentDto> comments,
                                BookingShortDto lastBooking, BookingShortDto nextBooking) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAvailable(item.isAvailable());
        dto.setOwnerId(item.getOwner().getId());
        dto.setRequestId(item.getRequestId());
        dto.setComments(comments);
        dto.setLastBooking(lastBooking);
        dto.setNextBooking(nextBooking);

        return dto;
    }
}
