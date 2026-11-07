package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    private static final long OWNER_ID = 1L;
    private static final long USER_ID = 2L;
    private static final long ITEM_ID = 10L;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private ItemCreateDto createDto(String name, String description, Boolean available) {
        return new ItemCreateDto(name, description, available);
    }

    private ItemDto updateDto(String name, String description, Boolean available) {
        ItemDto dto = new ItemDto();
        dto.setId(ITEM_ID);
        dto.setName(name);
        dto.setDescription(description);
        dto.setAvailable(available);
        return dto;
    }

    private Item storedItem() {
        Item item = new Item(ITEM_ID, OWNER_ID, "Дрель", "Аккумуляторная дрель", true, null);
        return item;
    }

    @Test
    void addItem_ownerExists_setsOwnerAndReturnsDto() {
        when(userRepository.findById(OWNER_ID)).thenReturn(new User());
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto result = itemService.addItem(OWNER_ID, createDto("Дрель", "Аккумуляторная дрель", true));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(OWNER_ID);

        assertThat(result.getName()).isEqualTo("Дрель");
        assertThat(result.getDescription()).isEqualTo("Аккумуляторная дрель");
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void addItem_unknownUser_throwsIllegalArgumentException() {
        when(userRepository.findById(OWNER_ID)).thenReturn(null);

        assertThatThrownBy(() -> itemService.addItem(OWNER_ID, createDto("Дрель", "Описание", true)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_byOwner_updatesOnlyNonNullFields() {
        Item existing = storedItem();
        when(itemRepository.findById(ITEM_ID)).thenReturn(existing);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // available = null: не должен затереть true
        ItemDto dto = updateDto("Перфоратор", null, null);

        ItemDto result = itemService.updateItem(ITEM_ID, OWNER_ID, dto);

        assertThat(result.getName()).isEqualTo("Перфоратор");
        assertThat(result.getDescription()).isEqualTo("Аккумуляторная дрель");
        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void updateItem_byNotOwner_throwsIllegalStateException() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(storedItem());

        assertThatThrownBy(() -> itemService.updateItem(ITEM_ID, USER_ID, updateDto("Перфоратор", null, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateItem_missingItem_throwsIllegalArgumentException() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(null);

        assertThatThrownBy(() -> itemService.updateItem(ITEM_ID, OWNER_ID, updateDto("Перфоратор", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void searchItems_returnsMappedDtos() {
        Item drill = new Item(10L, OWNER_ID, "Отвёртка", "Может заменить ДРЕЛЬ", true, null);
        Item other = new Item(11L, OWNER_ID, "Ключ", "Гаечный", true, null);
        when(itemRepository.search("дрель")).thenReturn(List.of(drill, other));

        List<ItemDto> result = itemService.searchItems("дрель");

        assertThat(result).hasSize(2); // сколько дал репозиторий — столько и вернул сервис
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getName()).isEqualTo("Отвёртка");
    }



    @Test
    void searchItems_blankText_returnsEmptyList() {
        assertThat(itemService.searchItems("   ")).isEmpty();
    }

    @Test
    void searchItems_nullText_returnsEmptyList() {
        assertThat(itemService.searchItems(null)).isEmpty();
    }

    @Test
    void searchItems_emptyText_returnsEmptyList() {
        assertThat(itemService.searchItems("")).isEmpty();
    }

    @Test
    void getItemById_missingItem_throwsIllegalArgumentException() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(null);

        assertThatThrownBy(() -> itemService.getItemById(ITEM_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
