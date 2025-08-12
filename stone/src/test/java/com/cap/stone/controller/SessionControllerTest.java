package com.cap.stone.controller;

import com.cap.stone.infra.opensearch.SessionDataService;
import com.cap.stone.infra.opensearch.SessionQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private SessionQueryService sessionQueryService;

    @Mock
    private SessionDataService sessionDataService;

    @InjectMocks
    private SessionController sessionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(sessionController).build();
    }

    @Test
    void testGetSessions_Success() throws Exception {
    
        String startDate = "2024-01-01";
        String endDate = "2024-01-31";
        int expectedUniqueSessions = 150;
        int expectedSnortHits = 75;
        
        List<Map<String, Object>> dailyData = Arrays.asList(
            createDailyDataEntry("2024-01-01", 10, 5),
            createDailyDataEntry("2024-01-02", 15, 8)
        );
        
        Map<String, Object> timeseriesResult = new HashMap<>();
        timeseriesResult.put("dailyData", dailyData);
        timeseriesResult.put("totalHits", 200);

        when(sessionQueryService.countUniqueCommunityIdsByDateRange(startDate, endDate))
            .thenReturn(expectedUniqueSessions);
        when(sessionQueryService.countSnortHitsByDateRange(startDate, endDate))
            .thenReturn(expectedSnortHits);
        when(sessionDataService.getDailyTimeseriesData(startDate, endDate))
            .thenReturn(timeseriesResult);

     
        mockMvc.perform(get("/api/sessions")
                .param("start", startDate)
                .param("end", endDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUniqueSessions", is(expectedUniqueSessions)))
                .andExpect(jsonPath("$.snortHits", is(expectedSnortHits)))
                .andExpect(jsonPath("$.timeseriesData", hasSize(2)))
                .andExpect(jsonPath("$.timeseriesData[0].date", is("2024-01-01")))
                .andExpect(jsonPath("$.timeseriesData[0].sessions", is(10)))
                .andExpect(jsonPath("$.timeseriesData[0].hits", is(5)))
                .andExpect(jsonPath("$.totalHitsInRange", is(200)))
                .andExpect(jsonPath("$.error").doesNotExist());

       
        verify(sessionQueryService).countUniqueCommunityIdsByDateRange(startDate, endDate);
        verify(sessionQueryService).countSnortHitsByDateRange(startDate, endDate);
        verify(sessionDataService).getDailyTimeseriesData(startDate, endDate);
    }

    @Test
    void testGetSessions_IOException() throws Exception {
        
        String startDate = "2024-01-01";
        String endDate = "2024-01-31";
        
        when(sessionQueryService.countUniqueCommunityIdsByDateRange(anyString(), anyString()))
            .thenThrow(new IOException("Database connection failed"));

     
        mockMvc.perform(get("/api/sessions")
                .param("start", startDate)
                .param("end", endDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error", is("Error fetching data")))
                .andExpect(jsonPath("$.totalUniqueSessions").doesNotExist())
                .andExpect(jsonPath("$.snortHits").doesNotExist())
                .andExpect(jsonPath("$.timeseriesData").doesNotExist())
                .andExpect(jsonPath("$.totalHitsInRange").doesNotExist());

        verify(sessionQueryService).countUniqueCommunityIdsByDateRange(startDate, endDate);
        // Verify that subsequent service calls are not made due to exception
        verify(sessionQueryService, never()).countSnortHitsByDateRange(anyString(), anyString());
        verify(sessionDataService, never()).getDailyTimeseriesData(anyString(), anyString());
    }

    @Test
    void testGetSessions_MissingRequiredParameters() throws Exception {
        // Test missing start parameter
        mockMvc.perform(get("/api/sessions")
                .param("end", "2024-01-31")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test missing end parameter
        mockMvc.perform(get("/api/sessions")
                .param("start", "2024-01-01")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Test missing both parameters
        mockMvc.perform(get("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Verify no service calls are made
        verifyNoInteractions(sessionQueryService, sessionDataService);
    }

    @Test
    void testGetSessions_ServiceCallsWithCorrectParameters() throws Exception {
      
        String startDate = "2024-03-01";
        String endDate = "2024-03-31";
        
        when(sessionQueryService.countUniqueCommunityIdsByDateRange(startDate, endDate))
            .thenReturn(100);
        when(sessionQueryService.countSnortHitsByDateRange(startDate, endDate))
            .thenReturn(50);
        when(sessionDataService.getDailyTimeseriesData(startDate, endDate))
            .thenReturn(Map.of("dailyData", Collections.emptyList(), "totalHits", 0));


        mockMvc.perform(get("/api/sessions")
                .param("start", startDate)
                .param("end", endDate));


        verify(sessionQueryService).countUniqueCommunityIdsByDateRange(eq(startDate), eq(endDate));
        verify(sessionQueryService).countSnortHitsByDateRange(eq(startDate), eq(endDate));
        verify(sessionDataService).getDailyTimeseriesData(eq(startDate), eq(endDate));
    }

    // Helper methods for creating test data
    private Map<String, Object> createDailyDataEntry(String date, int sessions, int hits) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("date", date);
        entry.put("sessions", sessions);
        entry.put("hits", hits);
        return entry;
    }
}
