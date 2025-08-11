package com.cap.stone.infra.opensearch;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cap.stone.service.OpenSearchClientService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Service
public class SessionQueryService {
    
    @Autowired
    private OpenSearchClientService clientService;
    
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
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .size(0);
                
                var response = clientService.executeSearch(indexPattern, sourceBuilder);
                totalCount += (int) response.getHits().getTotalHits().value();
                
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
                BoolQueryBuilder sidQuery = QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(SID_FIELD));
                
                SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(sidQuery)
                    .aggregation(AggregationBuilders.terms("community_ids")
                        .field(COMMUNITY_ID_FIELD)
                        .size(10000))
                    .size(0);
                
                var response = clientService.executeSearch(indexPattern, sourceBuilder);
                
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
        
        return uniqueCommunityIds.size();
    }
    
    private String buildIndexPattern(LocalDate date) {
        String indexSuffix = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        return "arkime_sessions*-" + indexSuffix;
    }
}
