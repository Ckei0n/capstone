package com.cap.stone.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImportService {
    @Autowired
    private OpenSearchClient client;
    
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
        List<BulkOperation> operations = new ArrayList<>();
        int count = 0;
        
        for (Map<String, Object> doc : documents) {
            String id = doc.get("id").toString();
            
            // Extract the data portion
            Map<String, Object> data = objectMapper.convertValue(
                doc.get("data"), new TypeReference<Map<String, Object>>() {}
            );
            
            IndexOperation<Map<String, Object>> indexOp = IndexOperation.of(i -> i
                .index(indexName)
                .id(id)
                .document(data)
            );
            
            operations.add(BulkOperation.of(op -> op.index(indexOp)));
            count++;
            
            // Flush in chunks
            if (count % BATCH_SIZE == 0) {
                flushBulk(operations);
                operations.clear();
            }
        }
        
        // Flush remaining documents
        if (!operations.isEmpty()) {
            flushBulk(operations);
        }
    }
    
    private void flushBulk(List<BulkOperation> operations) throws Exception {
        BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
        BulkResponse bulkResponse = client.bulk(bulkRequest);
        
        if (bulkResponse.errors()) {
            StringBuilder errorMsg = new StringBuilder("Bulk indexing failed: ");
            bulkResponse.items().forEach(item -> {
                if (item.error() != null) {
                    errorMsg.append(item.error().reason()).append("; ");
                }
            });
            throw new RuntimeException(errorMsg.toString());
        }
    }
}