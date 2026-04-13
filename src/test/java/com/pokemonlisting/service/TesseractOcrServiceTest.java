package com.pokemonlisting.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TesseractOcrServiceTest {

    @Test
    void testExtractText_Success() {
        TesseractOcrService service = new TesseractOcrService();
        String imagePath = "";
        String result = service.extractText(imagePath);

        System.out.println("Extracted text: " + result);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testExtractText_FileNotFound() {
        TesseractOcrService service = new TesseractOcrService();
        String imagePath = "C:\\nonexistent\\image.jpg";

        assertThrows(RuntimeException.class, () -> {
            service.extractText(imagePath);
        });
    }
}