package com.cap.stone.controller;

import com.cap.stone.infra.opensearch.SessionDataService;
import com.cap.stone.infra.opensearch.SessionQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SessionController {
    
    @Autowired
    private SessionQueryService sessionQueryService;
    
    @Autowired
    private SessionDataService sessionDataService;
    
    @GetMapping("/sessions")
    public Object getSessions(@RequestParam String start,
                             @RequestParam String end) {
        Map<String, Object> response = new HashMap<>();
        try {
            int totalUniqueSessions = sessionQueryService.countUniqueCommunityIdsByDateRange(start, end);
            response.put("totalUniqueSessions", totalUniqueSessions);
            
            int snortHits = sessionQueryService.countSnortHitsByDateRange(start, end);
            response.put("snortHits", snortHits);
            
            // Get daily aggregated timeseries data grouped by date
            Map<String, Object> timeseriesResult = sessionDataService.getDailyTimeseriesData(start, end);
            response.put("timeseriesData", timeseriesResult.get("dailyData"));
            response.put("totalHitsInRange", timeseriesResult.get("totalHits"));
        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "Error fetching data");
        }
        return response;
    }
    
    @GetMapping("/sessions/daily-details")
    public Object getDailySessionDetails(@RequestParam String start,
                                        @RequestParam String end,
                                        @RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> sessions = sessionDataService.getSessionsForSpecificDay(date);
            response.put("sessions", sessions);
            response.put("totalSessions", sessions.size());
            response.put("date", date);
        } catch (IOException e) {
            e.printStackTrace();
            response.put("error", "Error fetching daily session details");
        }
        return response;
    }
}