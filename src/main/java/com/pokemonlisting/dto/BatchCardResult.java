package com.pokemonlisting.dto;

public class BatchCardResult {
    private Long cardId;
    private Boolean success;
    private String cardName;
    private String cardNumber;
    private Double confidence;
    private String identificationMethod;
    private String errorMessage;
    private String identificationFailureReason;

    public BatchCardResult() {
    }

    // Constructor for successful identification
    public BatchCardResult(Long cardId, boolean success, String cardName, String cardNumber,
                           Double confidence, String identificationMethod, String identificationFailureReason) {
        this.cardId = cardId;
        this.success = success;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.confidence = confidence;
        this.identificationMethod = identificationMethod;
        this.identificationFailureReason = identificationFailureReason;
        this.errorMessage = null;
    }

    // Constructor for failed or skipped (no card data)
    public BatchCardResult(Long cardId, boolean success, String errorMessage) {
        this.cardId = cardId;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    // Constructor for skipped but already identified (has card data + reason)
    public BatchCardResult(Long cardId, boolean success, String cardName, String cardNumber,
                           Double confidence, String identificationMethod, String errorMessage,
                           String identificationFailureReason) {
        this.cardId = cardId;
        this.success = success;
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.confidence = confidence;
        this.identificationMethod = identificationMethod;
        this.errorMessage = errorMessage;
        this.identificationFailureReason = identificationFailureReason;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIdentificationFailureReason() {
        return identificationFailureReason;
    }

    public void setIdentificationFailureReason(String identificationFailureReason) {
        this.identificationFailureReason = identificationFailureReason;
    }
}
