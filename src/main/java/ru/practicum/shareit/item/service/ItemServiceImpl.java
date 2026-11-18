package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public ItemDto addItem(Long ownerId, ItemCreateDto dto) {
        var owner = userRepository.findById(ownerId);
        if (owner == null) {
            throw new IllegalArgumentException("Пользователь с id = " + ownerId + " не найден");
        }

        Item saved = itemRepository.save(ItemMapper.fromItemCreateDto(dto, ownerId));
        return ItemMapper.toItemDto(saved);
    }



    public ItemDto updateItem(Long itemId, Long userId, ItemDto dto) {
        Item existing = itemRepository.findById(itemId);
        if (existing == null) {
            throw new IllegalArgumentException("Вещь с id = " + itemId + " не найдена");
        }
        if (!existing.getOwner().equals(userId)) {
            throw new IllegalStateException(
                    "У пользователя с id = " + userId + " нет прав на редактирование вещи с id = " + itemId);
        }

        if (dto.getName() != null && !dto.getName().isBlank()) {
            existing.setName(dto.getName());
        }
        if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
            existing.setDescription(dto.getDescription());
        }
        if (dto.getAvailable() != null) {
            existing.setAvailable(dto.getAvailable());
        }

        Item updated = itemRepository.save(existing);
        return ItemMapper.toItemDto(updated);
    }


    public ItemDto getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Вещь с id = " + itemId + " не найдена");
        }
        return ItemMapper.toItemDto(item);
    }

    public List<ItemDto> getOwnerItems(Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("Пользователь с id = " + userId + " не найден");
        }

        List<Item> items = itemRepository.findByOwner(userId);
        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }


    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }


}
