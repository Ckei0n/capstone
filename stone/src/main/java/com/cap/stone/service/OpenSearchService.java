package com.cap.stone.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OpenSearchService {
    @Autowired
    @Qualifier("customOpenSearchClient")
    private RestHighLevelClient client;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int BATCH_SIZE = 500;
    
    public Map<String, Integer> indexDocumentsByIndex(List<Map<String, Object>> documents) throws Exception {
        // Group documents by their index field
        Map<String, List<Map<String, Object>>> documentsByIndex = documents.stream()
            .collect(Collectors.groupingBy(doc -> doc.get("index").toString()));
        
        Map<String, Integer> indexCounts = new HashMap<>();
        
        // Process each index separately
        for (Map.Entry<String, List<Map<String, Object>>> entry : documentsByIndex.entrySet()) {
            String indexName = entry.getKey();
            List<Map<String, Object>> indexDocuments = entry.getValue();
            
            indexDocuments(indexName, indexDocuments);
            indexCounts.put(indexName, indexDocuments.size());
        }
        
        return indexCounts;
    }
    
    public void indexDocuments(String indexName, List<Map<String, Object>> documents) throws Exception {
        BulkRequest bulkRequest = new BulkRequest();
        int count = 0;
        
        for (Map<String, Object> doc : documents) {
            String id = doc.get("id").toString();
            
            // Extract the data portion
            Map<String, Object> data = objectMapper.convertValue(
                doc.get("data"), new TypeReference<Map<String, Object>>() {}
            );
            
            IndexRequest request = new IndexRequest(indexName)
                    .id(id)
                    .source(objectMapper.writeValueAsString(data), XContentType.JSON);
            
            bulkRequest.add(request);
            count++;
            
            // Flush in chunks
            if (count % BATCH_SIZE == 0) {
                flushBulk(bulkRequest);
                bulkRequest = new BulkRequest();
            }
        }
        
        // Flush remaining documents
        if (bulkRequest.numberOfActions() > 0) {
            flushBulk(bulkRequest);
        }
    }
    
    private void flushBulk(BulkRequest bulkRequest) throws Exception {
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new RuntimeException("Bulk indexing failed: " + bulkResponse.buildFailureMessage());
        }
    }
}