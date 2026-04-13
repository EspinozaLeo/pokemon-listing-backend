package com.pokemonlisting.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GoogleVisionServiceTest {

    @Autowired
    private GoogleVisionService service;

    @Test
    void testExtractText_Success() {
        // Get test image from resources
        URL resource = getClass().getClassLoader()
                .getResource("test-images/imgsv1.jpg");
//                .getResource("test-images/sample-card.jpg");


        assertNotNull(resource, "Test image not found in src/test/resources/test-images/");

        // Properly handle URL encoding (spaces in path)
        String imagePath;
        try {
            imagePath = new File(resource.toURI()).getAbsolutePath();
        } catch (Exception e) {
            imagePath = new File(resource.getPath()).getAbsolutePath();
        }

        System.out.println("Testing with image: " + imagePath);

        String result = service.extractText(imagePath);

        System.out.println("=== GOOGLE VISION OCR RESULT ===");
        System.out.println(result);
        System.out.println("================================");

        assertNotNull(result, "Result should not be null");
        assertFalse(result.trim().isEmpty(), "Result should contain text");

        assertTrue(
                result.contains("/") ||
                        result.contains("HP") ||
                        result.length() > 10,
                "Should extract card-related text"
        );
    }

    @Test
    void testExtractText_FileNotFound() {
        String invalidPath = "C:\\nonexistent\\card.jpg";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            service.extractText(invalidPath);
        });

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testExtractText_NullPath() {
        assertThrows(RuntimeException.class, () -> {
            service.extractText(null);
        });
    }

    @Test
    void testExtractText_EmptyPath() {
        assertThrows(RuntimeException.class, () -> {
            service.extractText("");
        });
    }
}