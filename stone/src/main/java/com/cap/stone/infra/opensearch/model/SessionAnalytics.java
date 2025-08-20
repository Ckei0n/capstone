package com.cap.stone.infra.opensearch.model;

import java.util.*;

//analytics data container for session stats, contains all computed metrics from SessionAnalyticsService.
public class SessionAnalytics {
    private final List<Map<String, Object>> dailyData;
    private final int totalSnortHits;
    private final int totalUniqueSessions;
    private final Set<String> allUniqueCommunityIds;
    private final Set<Long> allUniqueSids;
    
    //New SessionAnalytics object with computed stats.
    public SessionAnalytics(List<Map<String, Object>> dailyData, int totalSnortHits, 
                           int totalUniqueSessions, Set<String> allUniqueCommunityIds, 
                           Set<Long> allUniqueSids) {
        this.dailyData = dailyData; // List of daily aggregated data points for timeseries visualization
        this.totalSnortHits = totalSnortHits; // Total count of Snort sids hits across the date range
        this.totalUniqueSessions = totalUniqueSessions; // Count of unique network sessions (community IDs) with sids
        this.allUniqueCommunityIds = allUniqueCommunityIds;// Set of all unique community IDs that had alerts
        this.allUniqueSids = allUniqueSids; // Set of all unique Snort signature IDs triggered
    }
    
    public List<Map<String, Object>> getDailyData() { return dailyData; }
    public int getTotalSnortHits() { return totalSnortHits; }
    public int getTotalUniqueSessions() { return totalUniqueSessions; }
    public Set<String> getAllUniqueCommunityIds() { return allUniqueCommunityIds; }
    public Set<Long> getAllUniqueSids() { return allUniqueSids; }
    
     //Converts to the API response format expected by the controller.
    public Map<String, Object> toApiResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("timeseriesData", dailyData);
        response.put("totalHitsInRange", totalSnortHits);
        response.put("totalUniqueSessions", totalUniqueSessions);
        return response;
    }
}