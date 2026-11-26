package ru.practicum.shareit.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.WrongStateException;
import ru.practicum.shareit.item.dto.ItemShortDto;
import ru.practicum.shareit.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";
    private static final Long USER_ID = 1L;
    private static final Long ITEM_ID = 2L;
    private static final Long BOOKING_ID = 3L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    void shouldReturn200WhenCreateBookingRequestIsValid() throws Exception {
        when(bookingService.createBooking(eq(USER_ID), any())).thenReturn(bookingDto());

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "itemId", ITEM_ID,
                                "start", LocalDateTime.now().plusHours(1),
                                "end", LocalDateTime.now().plusHours(2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(BOOKING_ID))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void shouldReturn400WhenItemIdIsMissing() throws Exception {
        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "start", LocalDateTime.now().plusHours(1),
                                "end", LocalDateTime.now().plusHours(2)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn400WhenItemIsUnavailable() throws Exception {
        when(bookingService.createBooking(eq(USER_ID), any()))
                .thenThrow(new WrongStateException("Вещь недоступна"));

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "itemId", ITEM_ID,
                                "start", LocalDateTime.now().plusHours(1),
                                "end", LocalDateTime.now().plusHours(2)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn400WhenStateIsInvalid() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, USER_ID)
                        .param("state", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {
        when(bookingService.getBookingById(BOOKING_ID, USER_ID))
                .thenThrow(new NotFoundException("Бронирование не найдено"));

        mockMvc.perform(get("/bookings/{bookingId}", BOOKING_ID)
                        .header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn200WhenBookingIsApproved() throws Exception {
        BookingDto approved = bookingDto();
        approved.setStatus(BookingStatus.APPROVED);
        when(bookingService.approveOrReject(BOOKING_ID, true, USER_ID)).thenReturn(approved);

        mockMvc.perform(patch("/bookings/{bookingId}", BOOKING_ID)
                        .header(USER_HEADER, USER_ID)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    private BookingDto bookingDto() {
        return BookingDto.builder()
                .id(BOOKING_ID)
                .item(new ItemShortDto(ITEM_ID, "Item"))
                .booker(new UserShortDto(USER_ID, "Booker"))
                .start(LocalDateTime.now().plusHours(1))
                .end(LocalDateTime.now().plusHours(2))
                .status(BookingStatus.WAITING)
                .build();
    }
}
