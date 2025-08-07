package com.cap.stone.infra.opensearch;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchClientWrapper {
    
    @Autowired
    @Qualifier("customOpenSearchClient")
    private RestHighLevelClient client;
    
    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final String COMMUNITY_ID_FIELD = "network.community_id.keyword";
    private static final String SID_FIELD = "extended.sid";
    private static final ZoneId SINGAPORE_ZONE = ZoneId.of("Asia/Singapore");
    
    // Get daily aggregated data by querying each date's index individually

    public Map<String, Object> executeDailyAggregationQueryByDateRange(String startDate, String endDate) throws IOException {
        System.out.println("=== DEBUG executeDailyAggregationQueryByDateRange ===");
        System.out.println("Date range: " + startDate + " to " + endDate);
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        Map<String, SessionGroup> singaporeDateGroups = new LinkedHashMap<>();
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexSuffix = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            String indexPattern = "arkime_sessions*-" + indexSuffix;
            
            System.out.println("Querying index pattern: " + indexPattern + " for date: " + currentDate);
            
            try {
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .size(10000)
                    .sort(TIMESTAMP_FIELD, SortOrder.DESC)
                    .fetchSource(getDetailedFields(), null);
                
                SearchResponse response = executeSearch(indexPattern, sourceBuilder);
                SearchHit[] hits = response.getHits().getHits();
                
                System.out.println("Found " + hits.length + " documents with SID in " + indexPattern);
                
                if (hits.length > 0) {
                    String singaporeDate = currentDate.toString();
                    SessionGroup group = singaporeDateGroups.computeIfAbsent(singaporeDate, k -> new SessionGroup(k));
                    
                    for (SearchHit hit : hits) {
                        Map<String, Object> processedSession = processHit(hit);
                        group.addSession(processedSession);
                        
                        String communityId = (String) processedSession.get("communityId");
                        Object sidObj = processedSession.get("sid");
                        
                        if (communityId != null) {
                            group.addCommunityIds(List.of(communityId));
                        }
                        
                        if (sidObj != null) {
                            group.addSids(extractSids(sidObj));
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
    
    // Get all sessions for a specific day by querying that day's index

    public List<Map<String, Object>> getSessionsForSpecificDay(String date) throws IOException {
        System.out.println("=== DEBUG getSessionsForSpecificDay ===");
        System.out.println("Singapore date: " + date);
        
        LocalDate localDate = LocalDate.parse(date);
        String indexSuffix = localDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        String indexPattern = "arkime_sessions*-" + indexSuffix;
        
        System.out.println("Querying index pattern: " + indexPattern);
        
        BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.existsQuery(SID_FIELD));
        
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
            .query(sidQuery)
            .size(10000)
            .sort(TIMESTAMP_FIELD, SortOrder.DESC)
            .fetchSource(getDetailedFields(), null);
        
        SearchResponse response = executeSearch(indexPattern, sourceBuilder);
        
        List<Map<String, Object>> sessions = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits()) {
            sessions.add(processHit(hit));
        }
        
        System.out.println("Found " + sessions.size() + " sessions for Singapore date " + date);
        return sessions;
    }
    
    // Count snort hits across date range by querying each date's index
    public int countSnortHitsByDateRange(String startDate, String endDate) throws IOException {
        System.out.println("=== DEBUG countSnortHitsByDateRange ===");
        System.out.println("Date range: " + startDate + " to " + endDate);
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        int totalCount = 0;
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexSuffix = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            String indexPattern = "arkime_sessions*-" + indexSuffix;
            
            try {
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .size(0);
                
                SearchResponse response = executeSearch(indexPattern, sourceBuilder);
                int dayCount = (int) response.getHits().getTotalHits().value();
                totalCount += dayCount;
                
                System.out.println("Date " + currentDate + " (index: " + indexPattern + "): " + dayCount + " snort hits");
                
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        System.out.println("Total snort hits across date range: " + totalCount);
        return totalCount;
    }
    
    // Count unique community IDs across date range by querying each date's index
    public int countUniqueCommunityIdsByDateRange(String startDate, String endDate) throws IOException {
        System.out.println("=== DEBUG countUniqueCommunityIdsByDateRange ===");
        System.out.println("Date range: " + startDate + " to " + endDate);
        
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        
        java.util.Set<String> uniqueCommunityIds = new java.util.HashSet<>();
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexSuffix = currentDate.format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
            String indexPattern = "arkime_sessions*-" + indexSuffix;
            
            try {
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .aggregation(AggregationBuilders.terms("community_ids")
                        .field(COMMUNITY_ID_FIELD)
                        .size(10000))
                    .size(0);
                
                SearchResponse response = executeSearch(indexPattern, sourceBuilder);
                
                if (response.getAggregations() != null) {
                    ParsedStringTerms communityTerms = response.getAggregations().get("community_ids");
                    communityTerms.getBuckets().forEach(bucket -> {
                        uniqueCommunityIds.add(bucket.getKeyAsString());
                    });
                }
                
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        int uniqueCount = uniqueCommunityIds.size();
        System.out.println("Total unique community IDs across date range: " + uniqueCount);
        return uniqueCount;
    }
    
    // Private helper methods
    
    private SearchResponse executeSearch(String indexPattern, SearchSourceBuilder sourceBuilder) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexPattern);
        searchRequest.source(sourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }
    
    private Map<String, Object> processHit(SearchHit hit) {
        Map<String, Object> session = hit.getSourceAsMap();
        session.put("_index", hit.getIndex());
        session.put("_id", hit.getId());
        
        Map<String, Object> processed = new HashMap<>();
        processed.put("timestamp", session.get(TIMESTAMP_FIELD));
        processed.put("indexName", session.get("_index"));
        processed.put("documentId", session.get("_id"));
        processed.put("communityId", getNestedValue(session, "network", "community_id"));
        processed.put("sid", getNestedValue(session, "extended", "sid"));
        processed.put("session", session.get("session"));
        processed.put("sourceIp", getNestedValue(session, "source", "ip"));
        processed.put("destIp", getNestedValue(session, "destination", "ip"));
        processed.put("sourcePort", getNestedValue(session, "source", "port"));
        processed.put("destPort", getNestedValue(session, "destination", "port"));
        processed.put("snortMessage", getNestedValue(session, "extended", "snort_message"));
        
        return processed;
    }
    
    private Object getNestedValue(Map<String, Object> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (current instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
            } else {
                return null;
            }
        }
        return current;
    }
    
    private String[] getDetailedFields() {
        return new String[]{
            TIMESTAMP_FIELD,
            "network.community_id",
            "extended.sid",
            "session",
            "source.ip",
            "destination.ip",
            "source.port",
            "destination.port",
            "extended.snort_message"
        };
    }
    
    private List<Long> extractSids(Object sidObj) {
        if (sidObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> sidList = (List<Object>) sidObj;
            return sidList.stream()
                .filter(s -> s instanceof Number)
                .map(s -> ((Number) s).longValue())
                .collect(Collectors.toList());
        } else if (sidObj instanceof Number) {
            return List.of(((Number) sidObj).longValue());
        }
        return new ArrayList<>();
    }
    
    private Map<String, Object> buildDailyDataResult(Map<String, SessionGroup> singaporeDateGroups) {
        List<Map<String, Object>> dailyData = new ArrayList<>();
        int totalHits = 0;
        
        for (SessionGroup group : singaporeDateGroups.values()) {
            System.out.println("Singapore Date: " + group.date + " has " + group.sessions.size() + " total snort hits");
            
            LocalDate localDate = LocalDate.parse(group.date);
            ZonedDateTime singaporeStartOfDay = localDate.atStartOfDay(SINGAPORE_ZONE);
            long timestampMs = singaporeStartOfDay.toInstant().toEpochMilli();
            
            // Calculate community ID hit counts for multi-line chart
            Map<String, Integer> communityIdHitCounts = group.getCommunityIdHitCounts();
            
            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", group.date);
            dayData.put("timestamp", timestampMs);
            dayData.put("singaporeDate", group.date);
            dayData.put("hitCount", group.sessions.size());
            dayData.put("communityIds", group.getUniqueCommunityIds());
            dayData.put("communityIdHitCounts", communityIdHitCounts); // NEW: Hit counts per community ID
            dayData.put("sids", group.getUniqueSids());
            dayData.put("sampleSessions", group.sessions.subList(0, Math.min(100, group.sessions.size())));
            dayData.put("hasMoreSessions", group.sessions.size() > 100);
            
            dailyData.add(dayData);
            totalHits += group.sessions.size();
        }
        
        System.out.println("Total actual hits: " + totalHits);
        
        Map<String, Object> result = new HashMap<>();
        result.put("dailyData", dailyData);
        result.put("totalHits", totalHits);
        
        return result;
    }
    
    // Helper class for grouping sessions by Singapore date
    private static class SessionGroup {
        String date;
        List<Map<String, Object>> sessions = new ArrayList<>();
        List<String> allCommunityIds = new ArrayList<>();
        List<Long> allSids = new ArrayList<>();
        
        SessionGroup(String date) {
            this.date = date;
        }
        
        void addSession(Map<String, Object> session) {
            sessions.add(session);
        }
        
        void addCommunityIds(List<String> communityIds) {
            allCommunityIds.addAll(communityIds);
        }
        
        void addSids(List<Long> sids) {
            allSids.addAll(sids);
        }
        
        List<String> getUniqueCommunityIds() {
            return allCommunityIds.stream().distinct().collect(Collectors.toList());
        }
        
        List<Long> getUniqueSids() {
            return allSids.stream().distinct().collect(Collectors.toList());
        }
        
        //Calculate hit counts per community ID for multi-line chart
        Map<String, Integer> getCommunityIdHitCounts() {
            Map<String, Integer> counts = new HashMap<>();
            
            for (Map<String, Object> session : sessions) {
                String communityId = (String) session.get("communityId");
                if (communityId != null) {
                    counts.put(communityId, counts.getOrDefault(communityId, 0) + 1);
                }
            }
            
            return counts;
        }
    }
}