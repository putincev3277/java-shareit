package ru.practicum.shareit.item.repository;

import ru.practicum.shareit.item.model.Item;

import java.util.List;

public interface ItemRepository {
    Item save(Item item);

    Item findById(Long id);

    List<Item> findAll();

    List<Item> findByOwner(Long ownerId);

    void delete(Long id);

    List<Item> search(String text);

}
