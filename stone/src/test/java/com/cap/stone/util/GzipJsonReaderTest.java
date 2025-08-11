package com.cap.stone.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
class GzipJsonReaderTest {

    @Test
    @DisplayName("Should successfully read valid JSON array from input stream")
    void testReadValidJsonArray() throws IOException {
        // Given
        String jsonData = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes());

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        
        Map<String, Object> firstPerson = result.get(0);
        assertEquals("John", firstPerson.get("name"));
        assertEquals(30, firstPerson.get("age"));
        
        Map<String, Object> secondPerson = result.get(1);
        assertEquals("Jane", secondPerson.get("name"));
        assertEquals(25, secondPerson.get("age"));
    }

    @Test
    @DisplayName("Should successfully read JSON array with nested objects")
    void testReadJsonArrayWithNestedObjects() throws IOException {
        // Given
        String jsonData = "[{\"user\":{\"id\":1,\"profile\":{\"email\":\"test@example.com\"}},\"active\":true}]";
        InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes());

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        Map<String, Object> item = result.get(0);
        assertTrue(item.containsKey("user"));
        assertTrue(item.containsKey("active"));
        assertEquals(true, item.get("active"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) item.get("user");
        assertEquals(1, user.get("id"));
    }

    @Test
    @DisplayName("Should successfully read GZIP compressed JSON data")
    void testReadGzipCompressedJson() throws IOException {
        // Given
        String jsonData = "[{\"compressed\":true,\"data\":\"test\"}]";
        byte[] gzippedData = compressString(jsonData);
        InputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzippedData));

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        Map<String, Object> item = result.get(0);
        assertEquals(true, item.get("compressed"));
        assertEquals("test", item.get("data"));
    }

    @Test
    @DisplayName("Should throw JsonMappingException for malformed JSON")
    void testMalformedJson() {
        // Given
        String malformedJson = "[{\"name\":\"John\",\"age\":}]"; // Missing value after age
        InputStream inputStream = new ByteArrayInputStream(malformedJson.getBytes());

        // When & Then
        assertThrows(JsonMappingException.class, () -> {
            GzipJsonReader.readGzipJsonStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should throw MismatchedInputException when JSON is not an array")
    void testNonArrayJsonFormat() {
        // Given
        String jsonObject = "{\"name\":\"John\",\"age\":30}";
        InputStream inputStream = new ByteArrayInputStream(jsonObject.getBytes());

        // When & Then
        assertThrows(MismatchedInputException.class, () -> {
            GzipJsonReader.readGzipJsonStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should handle null values in JSON")
    void testJsonWithNullValues() throws IOException {
        // Given
        String jsonData = "[{\"name\":\"John\",\"email\":null,\"age\":30}]";
        InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes());

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        Map<String, Object> person = result.get(0);
        assertEquals("John", person.get("name"));
        assertNull(person.get("email"));
        assertEquals(30, person.get("age"));
    }

    @Test
    @DisplayName("Should handle different data types in JSON")
    void testJsonWithDifferentDataTypes() throws IOException {
        // Given
        String jsonData = "[{\"string\":\"text\",\"number\":42,\"boolean\":true,\"decimal\":3.14,\"array\":[1,2,3]}]";
        InputStream inputStream = new ByteArrayInputStream(jsonData.getBytes());

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        Map<String, Object> item = result.get(0);
        assertEquals("text", item.get("string"));
        assertEquals(42, item.get("number"));
        assertEquals(true, item.get("boolean"));
        assertEquals(3.14, item.get("decimal"));
        
        @SuppressWarnings("unchecked")
        List<Integer> array = (List<Integer>) item.get("array");
        assertEquals(3, array.size());
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when input stream is null")
    void testNullInputStream() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            GzipJsonReader.readGzipJsonStream(null);
        });
    }

    @Test
    @DisplayName("Should throw JsonParseException for completely invalid JSON syntax")
    void testCompletelyInvalidJson() {
        // Given
        String invalidJson = "this is not json at all!";
        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes());

        // When & Then
        assertThrows(JsonParseException.class, () -> {
            GzipJsonReader.readGzipJsonStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should throw JsonMappingException for unclosed JSON structures")
    void testUnclosedJsonStructure() {
        // Given
        String unclosedJson = "[{\"name\":\"John\""; // Missing closing braces and bracket
        InputStream inputStream = new ByteArrayInputStream(unclosedJson.getBytes());

        // When & Then
        assertThrows(JsonMappingException.class, () -> {
            GzipJsonReader.readGzipJsonStream(inputStream);
        });
    }

    @Test
    @DisplayName("Should handle large JSON arrays efficiently")
    void testLargeJsonArray() throws IOException {
        // Given
        StringBuilder jsonBuilder = new StringBuilder("[");
        for (int i = 0; i < 1000; i++) {
            if (i > 0) jsonBuilder.append(",");
            jsonBuilder.append("{\"id\":").append(i).append(",\"name\":\"User").append(i).append("\"}");
        }
        jsonBuilder.append("]");
        
        InputStream inputStream = new ByteArrayInputStream(jsonBuilder.toString().getBytes());

        // When
        List<Map<String, Object>> result = GzipJsonReader.readGzipJsonStream(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1000, result.size());
        
        Map<String, Object> firstItem = result.get(0);
        assertEquals(0, firstItem.get("id"));
        assertEquals("User0", firstItem.get("name"));
        
        Map<String, Object> lastItem = result.get(999);
        assertEquals(999, lastItem.get("id"));
        assertEquals("User999", lastItem.get("name"));
    }

    // Helper method to compress string data with GZIP
    private byte[] compressString(String data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
            gzipOut.write(data.getBytes());
        }
        return baos.toByteArray();
    }
}