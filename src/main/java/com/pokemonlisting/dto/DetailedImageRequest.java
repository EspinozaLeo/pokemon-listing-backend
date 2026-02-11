package com.pokemonlisting.dto;

import com.pokemonlisting.model.ImageType;

public class DetailedImageRequest {
    private Long uploadedImageId;
    private ImageType imageType;
    private Integer displayOrder;

    public DetailedImageRequest() {
    }

    public DetailedImageRequest(Long uploadedImageId, ImageType imageType, Integer displayOrder) {
        this.uploadedImageId = uploadedImageId;
        this.imageType = imageType;
        this.displayOrder = displayOrder;
    }

    public Long getUploadedImageId() {
        return uploadedImageId;
    }

    public void setUploadedImageId(Long uploadedImageId) {
        this.uploadedImageId = uploadedImageId;
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
