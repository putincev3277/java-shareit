package ru.practicum.shareit.item.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User anotherOwner;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(user("Owner", "owner@mail.com"));
        anotherOwner = userRepository.save(user("Another", "another@mail.com"));
    }

    @Test
    void shouldFindItemsByOwnerOrderedById() {
        Item first = itemRepository.save(item(owner, "Drill", "Description", true));
        Item second = itemRepository.save(item(owner, "Ladder", "Description", true));
        itemRepository.save(item(anotherOwner, "Bike", "Description", true));

        List<Item> result = itemRepository.findByOwnerIdOrderByIdAsc(owner.getId());

        assertThat(result).extracting(Item::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    void shouldSearchByNameIgnoringCase() {
        itemRepository.save(item(owner, "DRILL", "Description", true));
        itemRepository.save(item(owner, "Ladder", "Description", true));

        List<Item> result = itemRepository.search("drill");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("DRILL");
    }

    @Test
    void shouldSearchByDescriptionIgnoringCase() {
        itemRepository.save(item(owner, "Screwdriver", "Can replace DRILL", true));
        itemRepository.save(item(owner, "Ladder", "Wooden", true));

        List<Item> result = itemRepository.search("drill");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Screwdriver");
    }

    @Test
    void shouldNotReturnUnavailableItemsInSearch() {
        itemRepository.save(item(owner, "Drill", "Description", false));

        assertThat(itemRepository.search("drill")).isEmpty();
    }

    private User user(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private Item item(User itemOwner, String name, String description, boolean available) {
        Item item = new Item();
        item.setOwner(itemOwner);
        item.setName(name);
        item.setDescription(description);
        item.setAvailable(available);
        return item;
    }
}
