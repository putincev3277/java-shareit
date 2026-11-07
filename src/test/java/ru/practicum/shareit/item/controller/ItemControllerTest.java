package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void addItem_withUserHeader_returnsDto() throws Exception {
        ItemDto dto = new ItemDto(10L, "Дрель", "Аккумуляторная дрель", true, null);
        when(itemService.addItem(eq(1L), any(ItemCreateDto.class))).thenReturn(dto);

        String body = "{\n"
                + "  \"name\": \"Дрель\",\n"
                + "  \"description\": \"Аккумуляторная дрель\",\n"
                + "  \"available\": true\n"
                + "}";

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Дрель"));
    }

    @Test
    void addItem_withoutUserHeader_returnsBadRequest() throws Exception {
        String body = "{\n"
                + "  \"name\": \"Дрель\",\n"
                + "  \"description\": \"Аккумуляторная дрель\",\n"
                + "  \"available\": true\n"
                + "}";

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void addItem_blankName_returnsBadRequest() throws Exception {
        String body = "{\n"
                + "  \"name\": \"\",\n"
                + "  \"description\": \"Аккумуляторная дрель\",\n"
                + "  \"available\": true\n"
                + "}";

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                // структура ошибки из ShareItExceptionHandler: "поле: сообщение"
                .andExpect(jsonPath("$.message").value("name: Название вещи обязательно"));

        verify(itemService, org.mockito.Mockito.never()).addItem(any(), any());
    }

    @Test
    void updateItem_passesOnlyProvidedFieldsToService() throws Exception {
        ItemDto updated = new ItemDto(10L, "Перфоратор", "Аккумуляторная дрель", true, null);
        when(itemService.updateItem(eq(10L), eq(1L), any(ItemDto.class))).thenReturn(updated);

        String body = "{\n"
                + "  \"name\": \"Перфоратор\"\n"
                + "}";

        mockMvc.perform(patch("/items/10")
                        .header(USER_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Перфоратор"));

        // проверяем, что контроллер передал только переданное поле, остальные — null
        ArgumentCaptor<ItemDto> captor = ArgumentCaptor.forClass(ItemDto.class);
        verify(itemService).updateItem(eq(10L), eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Перфоратор");
        assertThat(captor.getValue().getDescription()).isNull();
        assertThat(captor.getValue().getAvailable()).isNull();
    }

    @Test
    void searchItems_byText_returnsJsonArray() throws Exception {
        ItemDto drill = new ItemDto(10L, "Дрель", "Аккумуляторная дрель", true, null);
        when(itemService.searchItems("дрель")).thenReturn(List.of(drill));

        mockMvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Дрель"));
    }

    @Test
    void searchItems_emptyText_returnsEmptyArray() throws Exception {
        when(itemService.searchItems("")).thenReturn(List.of());

        mockMvc.perform(get("/items/search").param("text", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
