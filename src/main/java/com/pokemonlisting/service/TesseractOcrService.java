package com.pokemonlisting.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import java.io.File;

public class TesseractOcrService {
    private final Tesseract tesseract;

    public TesseractOcrService(){
        tesseract = new Tesseract();
        String tessDataPath = new File("src/main/resources/tessdata").getAbsolutePath();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
    }

    /**
     * Extracts text from an image file using OCR.
     *
     * @param imagePath Absolute path to the image file
     * @return Raw text extracted from the image
     * @throws RuntimeException if OCR fails or file not found
     */
    public String extractText(String imagePath) {
        if(imagePath == null || imagePath.isEmpty()) throw new RuntimeException("image path not found");

        File imageFile = new File(imagePath);

        if(!imageFile.exists()) throw new RuntimeException("image file does not exist");

        try {
            return tesseract.doOCR(imageFile);

        } catch (TesseractException e) {
            throw new RuntimeException("OCR failed for image: " + imagePath, e);
        }
    }
}
