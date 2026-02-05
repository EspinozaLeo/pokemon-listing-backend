package com.pokemonlisting.dto;

import java.util.List;

public class CreateBulkRequest {
    private List<Long> imageIds;

    public CreateBulkRequest(){
    }

    public CreateBulkRequest(List<Long> imageIds) {
        this.imageIds = imageIds;
    }

    public List<Long> getImageIds() {
        return imageIds;
    }

    public void setImageIds(List<Long> imageIds) {
        this.imageIds = imageIds;
    }
}
