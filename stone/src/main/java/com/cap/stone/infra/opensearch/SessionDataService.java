package com.cap.stone.infra.opensearch;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cap.stone.infra.opensearch.model.SessionGroup;
import com.cap.stone.service.OpenSearchClientService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SessionDataService {
    
    @Autowired
    private OpenSearchClientService clientService;
    
    @Autowired
    private SessionDataProcessor dataProcessor;
    
    private static final String SID_FIELD = "extended.sid";
    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");
    
    public Map<String, Object> getDailyTimeseriesData(String startDate, String endDate) throws IOException {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        Map<String, SessionGroup> singaporeDateGroups = new LinkedHashMap<>();
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexPattern = buildIndexPattern(currentDate);
            
            try {
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .size(10000)
                    .sort(TIMESTAMP_FIELD, SortOrder.DESC)
                    .fetchSource(dataProcessor.getDetailedFields(), null);
                
                var response = clientService.executeSearch(indexPattern, sourceBuilder);
                SearchHit[] hits = response.getHits().getHits();
                
                if (hits.length > 0) {
                    String singaporeDate = currentDate.toString();
                    SessionGroup group = singaporeDateGroups.computeIfAbsent(singaporeDate, SessionGroup::new);
                    
                    for (SearchHit hit : hits) {
                        Map<String, Object> processedSession = dataProcessor.processHit(hit);
                        group.addSession(processedSession);
                        
                        String communityId = (String) processedSession.get("communityId");
                        Object sidObj = processedSession.get("sid");
                        
                        if (communityId != null) {
                            group.addCommunityIds(List.of(communityId));
                        }
                        
                        if (sidObj != null) {
                            group.addSids(dataProcessor.extractSids(sidObj));
                        }
                    }
                }
                
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return buildDailyDataResult(singaporeDateGroups);
    }
    
    public List<Map<String, Object>> getSessionsForSpecificDay(String date) throws IOException {
        LocalDate localDate = LocalDate.parse(date);
        String indexPattern = buildIndexPattern(localDate);
        
        BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.existsQuery(SID_FIELD));
        
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .query(sidQuery)
            .size(10000)
            .sort(TIMESTAMP_FIELD, SortOrder.DESC)
            .fetchSource(dataProcessor.getDetailedFields(), null);
        
        var response = clientService.executeSearch(indexPattern, sourceBuilder);
        
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            sessions.add(dataProcessor.processHit(hit));
        }
        
        return sessions;
    }
    
    private String buildIndexPattern(LocalDate date) {
        String indexSuffix = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        return "arkime_sessions*-" + indexSuffix;
    }
    
    private Map<String, Object> buildDailyDataResult(Map<String, SessionGroup> singaporeDateGroups) {
        List<Map<String, Object>> dailyData = new ArrayList<>();
        int totalHits = 0;
        
        for (SessionGroup group : singaporeDateGroups.values()) {
            LocalDate localDate = LocalDate.parse(group.getDate());
            ZonedDateTime singaporeStartOfDay = localDate.atStartOfDay(SINGAPORE_ZONE);
            long timestampMs = singaporeStartOfDay.toInstant().toEpochMilli();
            
            Map<String, Integer> communityIdHitCounts = group.getCommunityIdHitCounts();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", group.getDate());
            dayData.put("timestamp", timestampMs);
            dayData.put("singaporeDate", group.getDate());
            dayData.put("hitCount", group.getSessionCount());
            dayData.put("communityIds", group.getUniqueCommunityIds());
            dayData.put("communityIdHitCounts", communityIdHitCounts);
            dayData.put("sids", group.getUniqueSids());
            dayData.put("sampleSessions", group.getSampleSessions(100));
            dayData.put("hasMoreSessions", group.hasMoreSessionsThan(100));
            
            dailyData.add(dayData);
            totalHits += group.getSessionCount();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("dailyData", dailyData);
        result.put("totalHits", totalHits);
        
        return result;
    }
}