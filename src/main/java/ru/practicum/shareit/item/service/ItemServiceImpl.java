package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
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
            throw new IllegalArgumentException("Пользователь не найден");
        }

        Item item = new Item();
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setAvailable(dto.getAvailable());
        item.setOwner(ownerId);

        Item saved = itemRepository.save(item);
        return ItemMapper.toItemDto(saved);
    }


    public ItemDto updateItem(Long itemId, Long userId, ItemDto dto) {
        Item existing = itemRepository.findById(itemId);
        if (existing == null) {
            throw new IllegalArgumentException("Вещь не найдена");
        }
        if (!existing.getOwner().equals(userId)) {
            throw new IllegalStateException("Вещь с id = " + itemId + " не найдена");

        }

        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getAvailable() != null) existing.setAvailable(dto.getAvailable());

        Item updated = itemRepository.save(existing);
        return ItemMapper.toItemDto(updated);
    }

    public ItemDto getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("Вещь не найдена");
        }
        return ItemMapper.toItemDto(item);
    }

    public List<ItemDto> getOwnerItems(Long userId) {
        List<Item> items = itemRepository.findByOwner(userId);
        return items.stream()
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    public List<ItemDto> searchItems(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String searchText = text.toLowerCase();
        return itemRepository.findAll().stream()
                .filter(Item::isAvailable)
                .filter(i -> contains(i.getName(), searchText)
                        || contains(i.getDescription(), searchText))
                .map(ItemMapper::toItemDto)
                .collect(Collectors.toList());
    }

    private boolean contains(String value, String text) {
        return value != null && value.toLowerCase().contains(text);
    }


}
