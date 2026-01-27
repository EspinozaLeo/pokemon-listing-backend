package com.pokemonlisting.dto;

import com.pokemonlisting.model.CardStatus;
import com.pokemonlisting.model.ImageType;

public class CardImageInfo {
    private Long imageId;
    private Long uploadedImageId;
    private String originalFilename;
    private ImageType imageType;
    private Integer displayOrder;

    public CardImageInfo(){
    }

    public CardImageInfo(Long imageId, Long uploadedImageId, String originalFilename, ImageType imageType, Integer displayOrder) {
        this.imageId = imageId;
        this.uploadedImageId = uploadedImageId;
        this.originalFilename = originalFilename;
        this.imageType = imageType;
        this.displayOrder = displayOrder;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public Long getUploadedImageId() {
        return uploadedImageId;
    }

    public void setUploadedImageId(Long uploadedImageId) {
        this.uploadedImageId = uploadedImageId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public ImageType getImageType() {
        return imageType;
    }

    public void setImageType(ImageType imageType) {
        this.imageType = imageType;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
