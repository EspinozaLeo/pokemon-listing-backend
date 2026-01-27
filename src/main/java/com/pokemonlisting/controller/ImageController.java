package com.pokemonlisting.controller;

import com.pokemonlisting.model.UploadedImage;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);
    private final UploadedImageRepository uploadedImageRepository;

    public ImageController(UploadedImageRepository uploadedImageRepository) {
        this.uploadedImageRepository = uploadedImageRepository;
    }

    @GetMapping("/list")
    public ResponseEntity<List<UploadedImage>> listAllImages() {
        List<UploadedImage> images = uploadedImageRepository.findAll();
        return ResponseEntity.ok(images);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> getUploadCount() {
        long count = uploadedImageRepository.count();
        Map<String, Object> response = new HashMap<>();

        response.put("totalUploads", count);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();

        //return 404 if image with given id is not found
        Optional<UploadedImage> optionalUploadedImage = uploadedImageRepository.findById(id);
        if (optionalUploadedImage.isEmpty()){
            response.put("success", false);
            response.put("message", "Image not found with id: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        UploadedImage image = optionalUploadedImage.get();
        String filePath = image.getFilePath();
        File file = new File(filePath);

        //delete physical file if it exists
        if(file.exists()){
            boolean deletedImage = file.delete();
            if (!deletedImage) {
                //file exists but couldn't be deleted
                response.put("success", false);
                response.put("message", "Failed to delete file from disk");
                return ResponseEntity.internalServerError().body(response);
            }
        } else {
            //file does not exist
            response.put("warning", "File was not found on disk (already deleted or missing)");
            log.warn("File not found on disk for image {}: {}", id, filePath);
            //continue to delete DB record
        }

        //delete DB record
        uploadedImageRepository.deleteById(id);
        //build success response
        response.put("success", true);
        response.put("message", "Image deleted successfully");
        response.put("deletedId", id);
        response.put("deletedFilename", image.getOriginalFilename());

        return ResponseEntity.ok(response);
    }
}