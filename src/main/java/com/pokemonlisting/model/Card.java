package com.pokemonlisting.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long id;
    private String cardName;
    private String setName;
    private String cardNumber;

    @Enumerated(EnumType.STRING)
    private CardStatus status;

    private String rarity;
    private Double confidence;
    private String identificationMethod;
    private Boolean needsReview;
    private String identificationFailureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Card() {
    }

    public Card(CardStatus status) {
        this.status = status;
    }

    public Card(String cardName, String setName, String cardNumber, CardStatus status) {
        this.cardName = cardName;
        this.setName = setName;
        this.cardNumber = cardNumber;
        this.status = status;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
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

    public String getIdentificationFailureReason() {
        return identificationFailureReason;
    }

    public void setIdentificationFailureReason(String identificationFailureReason) {
        this.identificationFailureReason = identificationFailureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}