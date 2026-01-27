package com.pokemonlisting.dto;

import com.pokemonlisting.model.CardStatus;

import java.time.LocalDateTime;
import java.util.List;

public class CardResponse {
    private Long cardId;
    private CardStatus status;
    private List<CardImageInfo> images;
    private LocalDateTime createdAt;

    public CardResponse(){
    }

    public CardResponse(Long cardId, CardStatus status, List<CardImageInfo> images, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.status = status;
        this.images = images;
        this.createdAt = createdAt;
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
}
