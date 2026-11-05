package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {
    ItemDto addItem(Long ownerId, ItemCreateDto dto);

    ItemDto updateItem(Long itemId, Long userId, ItemDto dto);

    ItemDto getItemById(Long itemId);

    List<ItemDto> getOwnerItems(Long userId);

    List<ItemDto> searchItems(String text);
}

