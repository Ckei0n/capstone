package com.cap.stone.infra.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.ExistsQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class SessionQueryService {
    
    @Autowired
    private OpenSearchClient client;
    
    private static final String SID_FIELD = "extended.sid";
    private static final String COMMUNITY_ID_FIELD = "network.community_id.keyword";
    
    public int countSnortHitsByDateRange(String startDate, String endDate) throws IOException {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int totalCount = 0;
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexPattern = buildIndexPattern(currentDate);
            
            try {
                Query sidQuery = Query.of(q -> q.bool(BoolQuery.of(b -> b
                    .must(Query.of(mq -> mq.exists(ExistsQuery.of(e -> e.field(SID_FIELD)))))
                )));
                
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexPattern)
                    .query(sidQuery)
                    .size(0)
                );
                
                
                @SuppressWarnings("unchecked")
                Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
                
                SearchResponse<Map<String, Object>> response = client.search(searchRequest, mapClass);
                totalCount += (int) response.hits().total().value();
                
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return totalCount;
    }
    
    public int countUniqueCommunityIdsByDateRange(String startDate, String endDate) throws IOException {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Set<String> uniqueCommunityIds = new HashSet<>();
        
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            String indexPattern = buildIndexPattern(currentDate);
            
            try {
                Query sidQuery = Query.of(q -> q.bool(BoolQuery.of(b -> b
                    .must(Query.of(mq -> mq.exists(ExistsQuery.of(e -> e.field(SID_FIELD)))))
                )));
                
                SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexPattern)
                    .query(sidQuery)
                    .aggregations("community_ids", Aggregation.of(a -> a
                        .terms(t -> t.field(COMMUNITY_ID_FIELD).size(10000))
                    ))
                    .size(0)
                );
                
                
                @SuppressWarnings("unchecked")
                Class<Map<String, Object>> mapClass = (Class<Map<String, Object>>) (Class<?>) Map.class;
                
                SearchResponse<Map<String, Object>> response = client.search(searchRequest, mapClass);
                
                if (response.aggregations() != null) {
                    StringTermsAggregate communityTerms = response.aggregations()
                        .get("community_ids")
                        .sterms();
                    
                    for (StringTermsBucket bucket : communityTerms.buckets().array()) {
                        uniqueCommunityIds.add(bucket.key());
                    }
                }
                
            } catch (Exception e) {
                System.out.println("No data found for index " + indexPattern + ": " + e.getMessage());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return uniqueCommunityIds.size();
    }
    
    private String buildIndexPattern(LocalDate date) {
        String indexSuffix = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        return "arkime_sessions*-" + indexSuffix;
    }
}