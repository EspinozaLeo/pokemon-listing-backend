package com.pokemonlisting.controller;

import com.pokemonlisting.model.UploadedImage;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/upload")
public class FileUploadController {

    //Spring automatically uses this for Dependency Injection since it has only
    //one constructor so no need for @Autowired annotation
    private final UploadedImageRepository uploadedImageRepository;

    public FileUploadController(UploadedImageRepository uploadedImageRepository) {
        this.uploadedImageRepository = uploadedImageRepository;
    }

    @Value("${upload.directory}")
    private String uploadDirectory;

    @PostMapping("/single")
    public ResponseEntity<Map<String, Object>> uploadSingleImage(
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file is not empty
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate file is an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "File must be an image");
                return ResponseEntity.badRequest().body(response);
            }

            // Generate unique filename to avoid conflicts
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Create upload directory if it doesn't exist
            File uploadDir = new File(uploadDirectory);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // Save the file
            Path filePath = Paths.get(uploadDirectory + uniqueFilename);
            Files.write(filePath, file.getBytes());

            UploadedImage image = new UploadedImage(
                    originalFilename,
                    uniqueFilename,
                    filePath.toString(),
                    file.getSize(),
                    contentType,
                    LocalDateTime.now()
            );

            UploadedImage savedImage = uploadedImageRepository.save(image);

            // Build success response
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("originalFilename", originalFilename);
            response.put("savedFilename", uniqueFilename);
            response.put("filePath", filePath.toString());
            response.put("fileSize", file.getSize());
            response.put("contentType", contentType);
            response.put("uploadTime", LocalDateTime.now());
            response.put("id", savedImage.getId());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleImages(
            @RequestParam("files") MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> uploadedFiles = new ArrayList<>();
        List<Map<String, Object>> failedFiles = new ArrayList<>();

        //create upload directory if it doesn't exist
        File uploadDir = new File(uploadDirectory);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        for (MultipartFile file : files) {
            try {

                //check if file is empty
                if (file.isEmpty()) {
                    throw new IOException("File is empty");
                }

                //check file is of image type
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IOException("File must be an image");
                }

                //generate unique filename
                String originalFilename = file.getOriginalFilename();
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

                //save the file
                Path filePath = Paths.get(uploadDirectory + uniqueFilename);
                Files.write(filePath, file.getBytes());

                //create uploadedImage entity
                UploadedImage image = new UploadedImage(
                        originalFilename,
                        uniqueFilename,
                        filePath.toString(),
                        file.getSize(),
                        contentType,
                        LocalDateTime.now()
                );

                //save to db
                UploadedImage savedImage = uploadedImageRepository.save(image);

                //add success info to uploadedFiles list
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("originalFilename", file.getOriginalFilename());
                fileInfo.put("savedFilename", savedImage.getSavedFilename());
                fileInfo.put("id", savedImage.getId());
                uploadedFiles.add(fileInfo);
            } catch (IOException e) {
                //add failure info to failedFiles list
                Map<String, Object> failureInfo = new HashMap<>();
                failureInfo.put("filename", file.getOriginalFilename());
                failureInfo.put("error", e.getMessage());
                failedFiles.add(failureInfo);
            }
        }
        int totalFiles = files.length;
        int successCount = uploadedFiles.size();
        int failedCount = failedFiles.size();
        response.put("success", true);
        response.put("message", "Processed " + totalFiles + " files: " + successCount + " successful, " + failedCount + " failed");
        response.put("totalFiles", totalFiles);
        response.put("successCount", successCount);
        response.put("failedCount", failedCount);
        response.put("uploadedFiles", uploadedFiles);
        response.put("failedFiles", failedFiles);
        response.put("uploadTime", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}