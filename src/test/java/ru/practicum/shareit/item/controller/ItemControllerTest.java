package ru.practicum.shareit.item.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemWithBookingsDto;
import ru.practicum.shareit.item.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 10L;
    private static final Long COMMENT_ID = 20L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void shouldReturn200WhenCreateRequestIsValid() throws Exception {
        ItemDto response = itemDto();
        when(itemService.addItem(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Drill",
                                "description", "Good drill",
                                "available", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID))
                .andExpect(jsonPath("$.name").value("Drill"));
    }

    @Test
    void shouldReturn400WhenItemNameIsBlank() throws Exception {
        mockMvc.perform(post("/items")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "",
                                "description", "Good drill",
                                "available", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn400WhenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Drill",
                                "description", "Good drill",
                                "available", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturnItemDetailsWithComments() throws Exception {
        ItemWithBookingsDto response = itemWithBookingsDto();
        when(itemService.getItemWithDetails(ITEM_ID, USER_ID)).thenReturn(response);

        mockMvc.perform(get("/items/{itemId}", ITEM_ID)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ITEM_ID))
                .andExpect(jsonPath("$.comments[0].authorName").value("Booker"));
    }

    @Test
    void shouldReturn404WhenItemNotFound() throws Exception {
        when(itemService.getItemWithDetails(ITEM_ID, USER_ID))
                .thenThrow(new NotFoundException("Вещь не найдена"));

        mockMvc.perform(get("/items/{itemId}", ITEM_ID)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn200WhenCommentIsValid() throws Exception {
        CommentDto response = commentDto();
        when(itemService.addComment(eq(ITEM_ID), eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(post("/items/{itemId}/comment", ITEM_ID)
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "Good item"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(COMMENT_ID))
                .andExpect(jsonPath("$.authorName").value("Booker"));
    }

    @Test
    void shouldReturn400WhenCommentTextIsBlank() throws Exception {
        mockMvc.perform(post("/items/{itemId}/comment", ITEM_ID)
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    private ItemDto itemDto() {
        ItemDto item = new ItemDto();
        item.setId(ITEM_ID);
        item.setName("Drill");
        item.setDescription("Good drill");
        item.setAvailable(true);
        item.setOwnerId(USER_ID);
        return item;
    }

    private ItemWithBookingsDto itemWithBookingsDto() {
        ItemWithBookingsDto item = new ItemWithBookingsDto();
        item.setId(ITEM_ID);
        item.setName("Drill");
        item.setDescription("Good drill");
        item.setAvailable(true);
        item.setOwnerId(USER_ID);
        item.setComments(List.of(commentDto()));
        return item;
    }

    private CommentDto commentDto() {
        CommentDto comment = new CommentDto();
        comment.setId(COMMENT_ID);
        comment.setText("Good item");
        comment.setAuthorName("Booker");
        comment.setCreated(LocalDateTime.now().minusHours(1));
        return comment;
    }
}
