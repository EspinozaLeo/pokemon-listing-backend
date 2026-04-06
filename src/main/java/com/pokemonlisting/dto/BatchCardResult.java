package com.pokemonlisting.dto;

public class BatchCardResult {
    private Long cardId;
    private String cardName;
    private String status;
    private String errorReason;

    public BatchCardResult() {
    }

    public BatchCardResult(Long cardId, String cardName, String status, String errorReason) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.status = status;
        this.errorReason = errorReason;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }
}
