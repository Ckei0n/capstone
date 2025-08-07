package com.cap.stone.controller;

import com.cap.stone.service.OpenSearchService;
import com.cap.stone.util.GzipJsonReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenSearchService openSearchService;

    @Test
    void shouldSuccessfullyImportSingleGzipFile() throws Exception {
        // Given
        List<Map<String, Object>> mockDocuments = createMockDocuments(2);
        Map<String, Integer> mockIndexCounts = Map.of("index1", 2);
        
        MockMultipartFile gzipFile = createGzipFile("test.gz", 
            "[{\"field\":\"value1\"},{\"field\":\"value2\"}]");

        // Mock static method
        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenReturn(mockDocuments);
            
            when(openSearchService.indexDocumentsByIndex(mockDocuments))
                .thenReturn(mockIndexCounts);

            // When & Then
            MvcResult result = mockMvc.perform(multipart("/api/import")
                    .file("files", gzipFile.getBytes())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/plain;charset=UTF-8"))
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                .contains("Successfully imported 2 documents from 1 file(s) into 1 indices:")
                .contains("- index1: 2 documents");

            verify(openSearchService).indexDocumentsByIndex(mockDocuments);
        }
    }

    @Test
    void shouldSuccessfullyImportMultipleGzipFiles() throws Exception {
        // Given
        List<Map<String, Object>> mockDocuments1 = createMockDocuments(2);
        List<Map<String, Object>> mockDocuments2 = createMockDocuments(3);
        
        Map<String, Integer> mockIndexCounts1 = Map.of("index1", 2);
        Map<String, Integer> mockIndexCounts2 = Map.of("index1", 1, "index2", 2);
        
        MockMultipartFile gzipFile1 = createGzipFile("test1.gz", 
            "[{\"field\":\"value1\"},{\"field\":\"value2\"}]");
        MockMultipartFile gzipFile2 = createGzipFile("test2.gz", 
            "[{\"field\":\"value3\"},{\"field\":\"value4\"},{\"field\":\"value5\"}]");

        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenReturn(mockDocuments1)
                       .thenReturn(mockDocuments2);
            
            when(openSearchService.indexDocumentsByIndex(mockDocuments1))
                .thenReturn(mockIndexCounts1);
            when(openSearchService.indexDocumentsByIndex(mockDocuments2))
                .thenReturn(mockIndexCounts2);

            // When & Then
            MvcResult result = mockMvc.perform(multipart("/api/import")
                    .file("files", gzipFile1.getBytes())
                    .file("files", gzipFile2.getBytes())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andReturn();

            String responseBody = result.getResponse().getContentAsString();
            assertThat(responseBody)
                .contains("Successfully imported 5 documents from 2 file(s) into 2 indices:")
                .contains("- index1: 3 documents")
                .contains("- index2: 2 documents");

            verify(openSearchService, times(2)).indexDocumentsByIndex(anyList());
        }
    }

    @Test
    void shouldReturnErrorWhenGzipJsonReaderThrowsException() throws Exception {
        // Given
        MockMultipartFile gzipFile = createGzipFile("test.gz", "invalid json");

        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenThrow(new RuntimeException("Invalid JSON format"));

            // When & Then
            mockMvc.perform(multipart("/api/import")
                    .file(gzipFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Error processing file: test.gz - Invalid JSON format")));

            verify(openSearchService, never()).indexDocumentsByIndex(anyList());
        }
    }

    @Test
    void shouldReturnErrorWhenOpenSearchServiceThrowsException() throws Exception {
        // Given
        List<Map<String, Object>> mockDocuments = createMockDocuments(1);
        MockMultipartFile gzipFile = createGzipFile("test.gz", "[{\"field\":\"value\"}]");

        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenReturn(mockDocuments);
            
            when(openSearchService.indexDocumentsByIndex(mockDocuments))
                .thenThrow(new RuntimeException("OpenSearch connection failed"));

            // When & Then
            mockMvc.perform(multipart("/api/import")
                    .file(gzipFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Error processing file: test.gz - OpenSearch connection failed")));
        }
    }

    @Test
    void shouldReturnErrorWhenFileInputStreamThrowsIOException() throws Exception {
        // Given - Create a file that will cause IOException when trying to read as GZIP
        MockMultipartFile invalidGzipFile = new MockMultipartFile(
            "files", "test.gz", "application/gzip", "not a gzip file".getBytes());

        // When & Then
        mockMvc.perform(multipart("/api/import")
                .file(invalidGzipFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error processing file: test.gz")));

        verify(openSearchService, never()).indexDocumentsByIndex(anyList());
    }

    @Test
    void shouldHandleEmptyFileArray() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/import")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
        
        verify(openSearchService, never()).indexDocumentsByIndex(anyList());
    }

    @Test
    void shouldHandleFileWithEmptyDocumentsList() throws Exception {
        // Given
        MockMultipartFile gzipFile = createGzipFile("empty.gz", "[]");

        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenReturn(List.of());
            
            when(openSearchService.indexDocumentsByIndex(List.of()))
                .thenReturn(new HashMap<>());

            // When & Then
            mockMvc.perform(multipart("/api/import")
                    .file("files", gzipFile.getBytes())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Successfully imported 0 documents from 1 file(s) into 0 indices:")));
        }
    }

    @Test
    void shouldHandleMixedSuccessAndFailureFiles() throws Exception {
        // Given
        MockMultipartFile validFile = createGzipFile("valid.gz", "[{\"field\":\"value\"}]");
        MockMultipartFile invalidFile = new MockMultipartFile(
            "files", "invalid.gz", "application/gzip", "invalid gzip content".getBytes());

        List<Map<String, Object>> mockDocuments = createMockDocuments(1);
        
        try (MockedStatic<GzipJsonReader> mockedReader = mockStatic(GzipJsonReader.class)) {
            mockedReader.when(() -> GzipJsonReader.readGzipJsonStream(any(InputStream.class)))
                       .thenReturn(mockDocuments);

            // When & Then - Should fail on the first invalid file
            mockMvc.perform(multipart("/api/import")
                    .file(invalidFile)
                    .file(validFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().string(containsString("Error processing file: invalid.gz")));
        }
    }

    // Helper methods
    private MockMultipartFile createGzipFile(String filename, String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(content.getBytes());
        }
        return new MockMultipartFile("files", filename, "application/gzip", baos.toByteArray());
    }

    private List<Map<String, Object>> createMockDocuments(int count) {
        return Arrays.stream(new int[count])
                .mapToObj(i -> {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("field", "value" + i);
                    return doc;
                })
                .toList();
    }
}