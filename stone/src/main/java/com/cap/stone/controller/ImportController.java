package com.cap.stone.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cap.stone.service.OpenSearchService;
import com.cap.stone.util.GzipJsonReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/api/import")
public class ImportController {
    @Autowired
    private OpenSearchService openSearchService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importGzipJson(@RequestParam("files") MultipartFile[] files) {
        int totalImported = 0;
        Map<String, Integer> indexCounts = new HashMap<>();
        
        for (MultipartFile file : files) {
            try (InputStream inputStream = new GZIPInputStream(file.getInputStream())) {
                List<Map<String, Object>> documents = GzipJsonReader.readGzipJsonStream(inputStream);
                Map<String, Integer> fileCounts = openSearchService.indexDocumentsByIndex(documents);
                
                // Merge counts from this file
                fileCounts.forEach((index, count) -> 
                    indexCounts.merge(index, count, Integer::sum)
                );
                
                totalImported += documents.size();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error processing file: " + file.getOriginalFilename() + " - " + e.getMessage());
            }
        }
        
        // Build response message
        StringBuilder responseMsg = new StringBuilder();
        responseMsg.append("Successfully imported ").append(totalImported).append(" documents from ")
                   .append(files.length).append(" file(s) into ").append(indexCounts.size()).append(" indices:\n");
        
        indexCounts.forEach((index, count) -> 
            responseMsg.append("- ").append(index).append(": ").append(count).append(" documents\n")
        );
        
        return ResponseEntity.ok(responseMsg.toString());
    }
}
