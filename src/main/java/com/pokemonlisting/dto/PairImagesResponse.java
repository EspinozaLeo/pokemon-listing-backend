package com.pokemonlisting.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PairImagesResponse {
    private boolean success;
    private String message;
    private int totalImages;
    private int pairedCount;
    private int unpairedCount;
    private List<CardResponse> pairedCards;
    private List<Long> unpairedImageIds;
    private LocalDateTime processedAt;

    public PairImagesResponse(){
    }

    public PairImagesResponse(boolean success, String message, int totalImages, int pairedCount, int unpairedCount, List<CardResponse> cardResponseList, List<Long> unpairedImageIds, LocalDateTime processedAt) {
        this.success = success;
        this.message = message;
        this.totalImages = totalImages;
        this.pairedCount = pairedCount;
        this.unpairedCount = unpairedCount;
        this.pairedCards = cardResponseList;
        this.unpairedImageIds = unpairedImageIds;
        this.processedAt = processedAt;
    }

    public PairImagesResponse(boolean failure, String message){
        this.success = failure;
        this.message = message;
    }
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotalImages() {
        return totalImages;
    }

    public void setTotalImages(int totalImages) {
        this.totalImages = totalImages;
    }

    public int getPairedCount() {
        return pairedCount;
    }

    public void setPairedCount(int pairedCount) {
        this.pairedCount = pairedCount;
    }

    public int getUnpairedCount() {
        return unpairedCount;
    }

    public void setUnpairedCount(int unpairedCount) {
        this.unpairedCount = unpairedCount;
    }

    public List<CardResponse> getPairedCards() {
        return pairedCards;
    }

    public void setPairedCards(List<CardResponse> pairedCards) {
        this.pairedCards = pairedCards;
    }

    public List<Long> getUnpairedImageIds() {
        return unpairedImageIds;
    }

    public void setUnpairedImageIds(List<Long> unpairedImageIds) {
        this.unpairedImageIds = unpairedImageIds;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
