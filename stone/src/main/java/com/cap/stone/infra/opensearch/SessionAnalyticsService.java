package com.cap.stone.infra.opensearch;

import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cap.stone.infra.opensearch.model.SessionAnalytics;
import com.cap.stone.infra.opensearch.model.SessionGroup;
import com.cap.stone.service.OpenSearchClientService;
import com.cap.stone.util.SingaporeTimezoneUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
public class SessionAnalyticsService {
    
    @Autowired
    private OpenSearchClientService clientService;
    
    @Autowired
    private SessionDataProcessor dataProcessor;
    
    @Autowired
    private SingaporeTimezoneUtil timezoneUtil;
    
    private static final String SID_FIELD = "extended.sid";
    private static final String TIMESTAMP_FIELD = "@timestamp";
    
     //method to get daily data, count sid hits and count unique network sessions
    public SessionAnalytics getSessionAnalytics(String startDate, String endDate) throws IOException {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        // Use LinkedHashMap to preserve chronological order
        Map<String, SessionGroup> singaporeDateGroups = new LinkedHashMap<>();
        
        // Tracking across all days
        Set<String> globalUniqueCommunityIds = new HashSet<>();
        int totalHits = 0;
        
        // Query each day individually
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            
            // Get index patterns for timezone boundary handling
            List<String> indexPatterns = timezoneUtil.getIndexPatternsForAnalytics(currentDate);
            
            for (String indexPattern : indexPatterns) {
                try {
                    // Query for documents with sid field
                    Query sidQuery = Query.of(q -> q.bool(BoolQuery.of(b -> b
                        .must(Query.of(mq -> mq.exists(ExistsQuery.of(e -> e.field(SID_FIELD)))))
                    )));
                    
                    // search with field filtering and sorting
                    SearchResponse<Map<String, Object>> response = clientService.executeSearch(
                        indexPattern, 
                        sidQuery, 
                        1000,                          // Max results per day
                        dataProcessor.getDetailedFields(), // Only fetch required fields
                        TIMESTAMP_FIELD,                   // Sort by timestamp
                        SortOrder.Desc                     // Most recent first
                    );
                    
                    List<Hit<Map<String, Object>>> hits = response.hits().hits();
                    
                    for (Hit<Map<String, Object>> hit : hits) {
                        Map<String, Object> processedSession = dataProcessor.processHit(hit);

                        // Convert UTC timestamp to Singapore time
                        Object timestampObj = processedSession.get("timestamp");
                        if (timestampObj != null) {
                            long singaporeTimestamp = timezoneUtil.convertToSingaporeTimestamp(timestampObj);
                            processedSession.put("timestamp", singaporeTimestamp);
                            
                            // Check if this session belongs to the current Singapore date
                            if (timezoneUtil.timestampBelongsToSingaporeDate(singaporeTimestamp, currentDate.toString())) {
                                String singaporeDate = currentDate.toString();
                                SessionGroup group = singaporeDateGroups.computeIfAbsent(singaporeDate, SessionGroup::new);
                                group.addSession(processedSession);
                                totalHits++;
                                
                                // Extract and track sid and community ID
                                String communityId = (String) processedSession.get("communityId");
                                Object sidObj = processedSession.get("sid");
                                
                                if (communityId != null) {
                                    group.addCommunityIds(List.of(communityId));
                                    globalUniqueCommunityIds.add(communityId);
                                }
                                
                                if (sidObj != null) {
                                    List<Long> sids = dataProcessor.extractSids(sidObj);
                                    group.addSids(sids);
                                }
                            }
                        }
                    }
                    
                } catch (Exception e) {
                    // Log but continue, some days may not have data
                    System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        // Build daily data for timeseries visualization
        List<Map<String, Object>> dailyData = buildDailyDataList(singaporeDateGroups);
        
        return new SessionAnalytics(
            dailyData, 
            totalHits, 
            globalUniqueCommunityIds.size()
        );
    }
    
    // Retrieves all network sessions with snort sids for a specific day.
    public List<Map<String, Object>> getSessionsForSpecificDay(String date) throws IOException {
        LocalDate localDate = LocalDate.parse(date);
        
        // Get index patterns for timezone boundary handling
        List<String> indexPatterns = timezoneUtil.getIndexPatternsForSingaporeDate(localDate);
        
        List<Map<String, Object>> sessions = new ArrayList<>();
        
        for (String indexPattern : indexPatterns) {
            try {
                // Query for all sessions with sids
                Query sidQuery = Query.of(q -> q.bool(BoolQuery.of(b -> b
                    .must(Query.of(mq -> mq.exists(ExistsQuery.of(e -> e.field(SID_FIELD)))))
                )));
                
                SearchResponse<Map<String, Object>> response = clientService.executeSearch(
                    indexPattern, 
                    sidQuery, 
                    1000,                       
                    dataProcessor.getDetailedFields(),
                    TIMESTAMP_FIELD,
                    SortOrder.Desc
                );
                
                // Process all hits into session objects
                for (Hit<Map<String, Object>> hit : response.hits().hits()) {
                    Map<String, Object> processedSession = dataProcessor.processHit(hit);

                    // Convert UTC timestamp to Singapore time
                    Object timestampObj = processedSession.get("timestamp");
                    if (timestampObj != null) {
                        long singaporeTimestamp = timezoneUtil.convertToSingaporeTimestamp(timestampObj);
                        processedSession.put("timestamp", singaporeTimestamp);
                        
                        // Only include if this session belongs to the requested Singapore date
                        if (timezoneUtil.timestampBelongsToSingaporeDate(singaporeTimestamp, date)) {
                            sessions.add(processedSession);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
        }

        return sessions;
    }
    
    // Transforms grouped session data into the daily data list for timeseries visualization. Creates data points with aggregated stats for each day.
    private List<Map<String, Object>> buildDailyDataList(Map<String, SessionGroup> singaporeDateGroups) {
        List<Map<String, Object>> dailyData = new ArrayList<>();
        
        for (SessionGroup group : singaporeDateGroups.values()) {
            LocalDate localDate = LocalDate.parse(group.getDate());
            
            
            long timestampMs = timezoneUtil.getSingaporeStartOfDayTimestamp(localDate);
            
            Map<String, Integer> communityIdHitCounts = group.getCommunityIdHitCounts();
            
            // Build data point for this day
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("timestamp", timestampMs);                    // Unix timestamp for charts
            dayData.put("singaporeDate", group.getDate());            // singapore date
            dayData.put("hitCount", group.getSessionCount());         // Total sessions
            dayData.put("communityIds", group.getUniqueCommunityIds()); // Unique network sessions
            dayData.put("communityIdHitCounts", communityIdHitCounts); // count appearance of community id per network session
            dayData.put("sids", group.getUniqueSids());               // Unique sids
            dayData.put("sampleSessions", group.getSampleSessions(100)); // Sample data
            dayData.put("hasMoreSessions", group.hasMoreSessionsThan(100)); // Pagination
            
            dailyData.add(dayData);
        }
        
        return dailyData;
    }
}