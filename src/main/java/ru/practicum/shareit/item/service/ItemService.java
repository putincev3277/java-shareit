package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;

import java.util.List;

public interface ItemService {

    ItemDto addItem(Long ownerId, ItemCreateDto dto);

    ItemDto updateItem(Long itemId, Long userId, ItemUpdateDto dto);

    ItemWithBookingsDto getItemWithDetails(Long itemId, Long userId);

    List<ItemDto> searchItems(String text);

    List<ItemDto> getOwnerItemsWithBookingDates(Long ownerId);

    CommentDto addComment(Long itemId, Long userId, CommentCreateDto dto);
}
