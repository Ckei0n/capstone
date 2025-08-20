package com.cap.stone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.cap.stone.service.ImportService;
import com.cap.stone.util.GzipJsonReader;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

//  Handles file upload and bulk import of compressed JSON data into OpenSearch indices.
@RestController
@RequestMapping("/api/import")
public class ImportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportController.class); //log errors or messages

    @Autowired
    private ImportService importService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE) //accepts mutiple file uploads
    public ResponseEntity<String> importGzipJson(@RequestParam("files") MultipartFile[] files) {
        int totalImported = 0;
        Map<String, Integer> indexCounts = new HashMap<>(); // Track documents per index
        
        // Process each uploaded file
        for (MultipartFile file : files) {
            try (InputStream inputStream = new GZIPInputStream(file.getInputStream())) {
                // Decompress and parse JSON documents from the gzipped file
                List<Map<String, Object>> documents = GzipJsonReader.readGzipJsonStream(inputStream);

                // Import documents and get count per index
                Map<String, Integer> fileCounts = importService.indexDocumentsByIndex(documents);
                
                // Merge counts from this file into total counts
                fileCounts.forEach((index, count) -> 
                    indexCounts.merge(index, count, Integer::sum)
                );
                
                totalImported += documents.size();
            } catch (Exception e) {
                logger.error("Error processing file: {} - {}", file.getOriginalFilename(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing uploaded file. Please check the file format and try again.");
}
        }
        
        // Build response message with import stats
        StringBuilder responseMsg = new StringBuilder();
        responseMsg.append("Successfully imported ").append(totalImported).append(" documents from ")
                   .append(files.length).append(" file(s) into ").append(indexCounts.size()).append(" indices:\n");
        
        //per-index breakdown
        indexCounts.forEach((index, count) -> 
            responseMsg.append("- ").append(index).append(": ").append(count).append(" documents\n")
        );
        
        return ResponseEntity.ok(responseMsg.toString());
    }
}
