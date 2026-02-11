package com.pokemonlisting.dto;

import java.util.List;

public class CreateDetailedCardRequest {
    private List<DetailedImageRequest> images;

    public CreateDetailedCardRequest() {
    }

    public CreateDetailedCardRequest(List<DetailedImageRequest> images) {
        this.images = images;
    }

    public List<DetailedImageRequest> getImages() {
        return images;
    }

    public void setImages(List<DetailedImageRequest> images) {
        this.images = images;
    }
}
