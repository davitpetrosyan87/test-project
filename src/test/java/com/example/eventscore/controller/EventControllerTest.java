package com.example.eventscore.controller;

import com.example.eventscore.service.EventSchedulerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventSchedulerService eventSchedulerService;

    @Test
    void updateStatus_validLiveRequest_returns200() throws Exception {
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "match-1", "status": "live"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value("match-1"))
                .andExpect(jsonPath("$.status").value("live"))
                .andExpect(jsonPath("$.message").value("Event status updated successfully"));

        verify(eventSchedulerService).updateStatus("match-1", "live");
    }

    @Test
    void updateStatus_validNotLiveRequest_returns200() throws Exception {
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "match-1", "status": "not live"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("not live"));

        verify(eventSchedulerService).updateStatus("match-1", "not live");
    }

    @Test
    void updateStatus_missingEventId_returns400() throws Exception {
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "live"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verifyNoInteractions(eventSchedulerService);
    }

    @Test
    void updateStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId": "match-1", "status": "paused"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.status").exists());

        verifyNoInteractions(eventSchedulerService);
    }

    @Test
    void updateStatus_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/events/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventSchedulerService);
    }
}
