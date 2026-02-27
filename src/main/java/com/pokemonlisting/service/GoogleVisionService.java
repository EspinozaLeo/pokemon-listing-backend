package com.pokemonlisting.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleVisionService {

    private final GoogleCredentials credentials;

    public GoogleVisionService(
            @Value("${google.vision.credentials.path:}") String credentialsPath
    ) {
        try {
            // Try environment variable first
            String envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
            String pathToUse = (envPath != null && !envPath.isEmpty())
                    ? envPath
                    : credentialsPath;

            if (pathToUse == null || pathToUse.isEmpty()) {
                throw new IllegalStateException(
                        "Google Cloud credentials not configured. " +
                                "Set GOOGLE_APPLICATION_CREDENTIALS environment variable or " +
                                "google.vision.credentials.path in application.properties"
                );
            }

            credentials = GoogleCredentials.fromStream(
                    new FileInputStream(pathToUse)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google Cloud credentials", e);
        }
    }

    /**
     * Extracts text from an image using Google Cloud Vision OCR.
     *
     * @param imagePath Absolute path to the image file
     * @return Raw text extracted from the image
     * @throws RuntimeException if OCR fails or file not found
     */
    public String extractText(String imagePath) {

        if(imagePath == null || imagePath.isEmpty()){
            throw new RuntimeException("Image path cannot be null or empty");
        }

        Path path = Paths.get(imagePath);
        if(!Files.exists(path)){
            throw new RuntimeException("File not found: " + imagePath);
        }

        try {
            byte[] imageData = Files.readAllBytes(path);

            //convert to ByteString (Google's format)
            ByteString byteString = ByteString.copyFrom(imageData);

            Image image = Image.newBuilder().setContent(byteString).build();

            //build Feature (what we want: TEXT_DETECTION)
            Feature feature = Feature.newBuilder()
                    .setType(Feature.Type.TEXT_DETECTION)
                    .build();

            //build AnnotateImageRequest
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feature)
                    .setImage(image)
                    .build();

            List<AnnotateImageRequest> requests = new ArrayList<>();
            requests.add(request);

            ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(() -> this.credentials)  // Use 'this.credentials'
                    .build();

            try (ImageAnnotatorClient client = ImageAnnotatorClient.create(settings)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                AnnotateImageResponse imageResponse = response.getResponses(0);

                if (imageResponse.hasError()) {
                    throw new RuntimeException("Vision API error: " + imageResponse.getError().getMessage());
                }
                List<EntityAnnotation> annotations = imageResponse.getTextAnnotationsList();
                if (annotations.isEmpty()) {
                    return "";
                }
                String extractedText = annotations.get(0).getDescription();
                return extractedText;
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read image file: " + imagePath, e);
        }
    }
}