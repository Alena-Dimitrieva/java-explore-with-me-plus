package ru.practicum.stat.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.stat.dto.EndpointHitDto;
import ru.practicum.stat.server.service.StatService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatController.class)
class StatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StatService statsService;

    @Test
    void hit_whenValidDto_thenStatusCreated() throws Exception {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.168.1.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .content(objectMapper.writeValueAsString(hitDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        verify(statsService).saveHit(any(EndpointHitDto.class));
    }

    @Test
    void hit_whenInvalidIp_thenStatusBadRequest() throws Exception {
        EndpointHitDto hitDto = EndpointHitDto.builder()
                .app("ewm")
                .uri("/events/1")
                // .ip("192.168.1.1") // Отсутствует ip
                .timestamp(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .content(objectMapper.writeValueAsString(hitDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.reason").value("Bad Request"));
    }

    @Test
    void getStats_whenStartAfterEnd_thenStatusBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2025-01-01 00:00:00")
                        .param("end", "2023-01-01 00:00:00") // Дата окончания раньше начала
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Дата окончания не может быть раньше даты начала"));
    }

    @Test
    void getStats_whenValidParams_thenStatusOk() throws Exception {
        when(statsService.getStats(any(), any(), any(), anyBoolean()))
                .thenReturn(List.of());

        mockMvc.perform(get("/stats")
                        .param("start", "2023-01-01 00:00:00")
                        .param("end", "2023-01-02 00:00:00")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}