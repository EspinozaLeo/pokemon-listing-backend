package com.pokemonlisting.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import java.time.LocalDateTime;

@Entity(name="uploadedimage")
@Table(name="uploaded_images")
public class UploadedImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="original_filename")
    private String originalFilename;
    private String savedFilename;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;

    //default constructor required by JPA
    public UploadedImage(){
    }

    //constructor
    public UploadedImage(String originalFilename, String savedFilename, String filePath,
                         Long fileSize, String contentType, LocalDateTime uploadedAt){
        this.originalFilename = originalFilename;
        this.savedFilename = savedFilename;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getSavedFilename() {
        return savedFilename;
    }

    public void setSavedFilename(String savedFilename) {
        this.savedFilename = savedFilename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}