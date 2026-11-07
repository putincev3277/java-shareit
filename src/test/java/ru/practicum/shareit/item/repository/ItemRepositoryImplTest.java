package ru.practicum.shareit.item.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryImplTest {

    private ItemRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ItemRepositoryImpl();
    }

    private Item item(Long ownerId, String name, boolean available) {
        Item item = new Item();
        item.setOwner(ownerId);
        item.setName(name);
        item.setDescription(name + " description");
        item.setAvailable(available);
        return item;
    }

    @Test
    void save_newItem_assignsIncrementingIds() {
        Item first = repository.save(item(1L, "Drill", true));
        Item second = repository.save(item(2L, "Ladder", true));

        assertThat(first.getId()).isEqualTo(1L);
        assertThat(second.getId()).isEqualTo(2L);
    }

    @Test
    void save_existingItem_keepsIdAndUpdatesData() {
        Item saved = repository.save(item(1L, "Drill", true));
        Long id = saved.getId();

        saved.setName("Power Drill");
        saved.setAvailable(false);
        Item result = repository.save(saved);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getName()).isEqualTo("Power Drill");
        assertThat(result.isAvailable()).isFalse();
        assertThat(repository.findById(id)).isSameAs(saved);
    }

    @Test
    void findById_missingItem_returnsNull() {
        assertThat(repository.findById(999L)).isNull();
    }

    @Test
    void findByOwner_returnsOnlyOwnerItems() {
        Item drill = repository.save(item(1L, "Drill", true));
        Item ladder = repository.save(item(1L, "Ladder", true));
        repository.save(item(2L, "Bike", true));

        List<Item> ownerItems = repository.findByOwner(1L);

        assertThat(ownerItems).containsExactlyInAnyOrder(drill, ladder);
    }

    @Test
    void findByOwner_unknownOwner_returnsEmptyList() {
        repository.save(item(1L, "Drill", true));

        assertThat(repository.findByOwner(42L)).isEmpty();
    }

    @Test
    void delete_existingItem_removesItem() {
        Item saved = repository.save(item(1L, "Drill", true));

        repository.delete(saved.getId());

        assertThat(repository.findById(saved.getId())).isNull();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void findAll_returnsAllItems() {
        repository.save(item(1L, "Drill", true));
        repository.save(item(2L, "Ladder", false));

        List<Item> all = repository.findAll();

        assertThat(all).hasSize(2)
                .extracting(Item::getName)
                .containsExactlyInAnyOrder("Drill", "Ladder");
    }

    @Test
    void search_byDescription_ignoresCase() {
        repository.save(new Item(null, 1L, "Отвёртка", "Может заменить ДРЕЛЬ", true, null));
        repository.save(new Item(null, 1L, "Ключ", "Гаечный", true, null));
        repository.save(new Item(null, 1L, "Дрель", "Мощная", false, null)); // недоступная

        List<Item> result = repository.search("дрель");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Отвёртка");
    }

    @Test
    void search_byName_ignoresCase() {
        repository.save(new Item(null, 1L, "ДРЕЛЬ ударная", "Мощная", true, null));
        repository.save(new Item(null, 1L, "Ключ", "Гаечный", true, null));

        List<Item> result = repository.search("дрель");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("ДРЕЛЬ ударная");
    }

}
