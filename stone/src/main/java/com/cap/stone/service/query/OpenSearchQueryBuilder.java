package com.cap.stone.service.query;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchQueryBuilder {
    
    private static final String TIMESTAMP_FIELD = "@timestamp";
    private static final String SID_FIELD = "extended.sid";
    
    public BoolQueryBuilder createDateRangeQuery(String startDate, String endDate) {
        long startEpoch = parseDate(startDate);
        long endEpoch = parseDate(endDate);
        
        return QueryBuilders.boolQuery()
            .must(QueryBuilders.rangeQuery(TIMESTAMP_FIELD).gte(startEpoch).lte(endEpoch));
    }
    
    public BoolQueryBuilder createSnortHitsQuery(String startDate, String endDate) {
        return createDateRangeQuery(startDate, endDate)
            .must(QueryBuilders.existsQuery(SID_FIELD));
    }
    
    private long parseDate(String dateString) {
    long epoch = LocalDate.parse(dateString)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli();
    
    return epoch;
    }
}
