package ru.practicum.shareit.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final Long USER_ID = 1L;
    private static final String NAME = "Alice";
    private static final String EMAIL = "alice@mail.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void shouldReturn201WhenCreateUserRequestIsValid() throws Exception {
        when(userService.create(any())).thenReturn(userDto());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", NAME,
                                "email", EMAIL))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", NAME,
                                "email", "wrong-email"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(USER_ID))
                .thenThrow(new NotFoundException("Пользователь не найден"));

        mockMvc.perform(get("/users/{userId}", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn200WhenUserIsUpdated() throws Exception {
        UserDto updated = userDto();
        updated.setName("Alice Updated");
        when(userService.updateUser(eq(USER_ID), any())).thenReturn(updated);

        mockMvc.perform(patch("/users/{userId}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Alice Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"));
    }

    private UserDto userDto() {
        return new UserDto(USER_ID, NAME, EMAIL);
    }
}
