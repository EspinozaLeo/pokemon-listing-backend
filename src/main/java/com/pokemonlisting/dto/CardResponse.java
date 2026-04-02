package com.pokemonlisting.dto;

import com.pokemonlisting.model.CardStatus;

import java.time.LocalDateTime;
import java.util.List;

public class CardResponse {
    private Long cardId;
    private CardStatus status;
    private List<CardImageInfo> images;
    private LocalDateTime createdAt;
    private String cardName;
    private String setName;
    private String cardNumber;
    private String rarity;
    private Double confidence;
    private String identificationMethod;
    private Boolean needsReview;

    public CardResponse(){
    }

    // Backward-compatible constructor for existing callers (pair, list, getById, etc.)
    public CardResponse(Long cardId, CardStatus status, List<CardImageInfo> images, LocalDateTime createdAt) {
        this(cardId, status, images, createdAt, null, null, null, null, null, null, null);
    }

    // Full constructor used by identifyCard endpoint
    public CardResponse(Long cardId, CardStatus status, List<CardImageInfo> images, LocalDateTime createdAt,
                        String cardName, String setName, String cardNumber,
                        String rarity, Double confidence, String identificationMethod, Boolean needsReview) {
        this.cardId = cardId;
        this.status = status;
        this.images = images;
        this.createdAt = createdAt;
        this.cardName = cardName;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.rarity = rarity;
        this.confidence = confidence;
        this.identificationMethod = identificationMethod;
        this.needsReview = needsReview;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public List<CardImageInfo> getImages() {
        return images;
    }

    public void setImages(List<CardImageInfo> images) {
        this.images = images;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getIdentificationMethod() {
        return identificationMethod;
    }

    public void setIdentificationMethod(String identificationMethod) {
        this.identificationMethod = identificationMethod;
    }

    public Boolean getNeedsReview() {
        return needsReview;
    }

    public void setNeedsReview(Boolean needsReview) {
        this.needsReview = needsReview;
    }
}
