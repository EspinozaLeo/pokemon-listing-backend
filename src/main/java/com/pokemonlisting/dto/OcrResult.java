package com.pokemonlisting.dto;

public class OcrResult {

    private String cardName;
    private String cardNumber;
    private String totalCards;
    private String setCode;            //may be null or mangled
    private String tcgdexSetId;
    private String copyrightYear;
    private String dexId;              //as extracted, may be null for Trainers
    private Integer normalizedDexId;
    private String expectedName;       //"Eevee" (from Pokédex lookup)
    private Boolean nameMatchesDex;
    private ConfidenceLevel confidence;

    public enum ConfidenceLevel {
        HIGH,    // Card number + validated name + set identified
        MEDIUM,  // Card number + some validation/fallback
        LOW      // Missing critical data
    }

    // No-arg constructor
    public OcrResult() {
    }

    // All-args constructor
    public OcrResult(String cardName, String cardNumber, String totalCards,
                     String setCode, String tcgdexSetId, String copyrightYear,
                     String dexId, Integer normalizedDexId, String expectedName,
                     Boolean nameMatchesDex, ConfidenceLevel confidence) {
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.totalCards = totalCards;
        this.setCode = setCode;
        this.tcgdexSetId = tcgdexSetId;
        this.copyrightYear = copyrightYear;
        this.dexId = dexId;
        this.normalizedDexId = normalizedDexId;
        this.expectedName = expectedName;
        this.nameMatchesDex = nameMatchesDex;
        this.confidence = confidence;
    }

    // Getters and Setters
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

    public String getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(String totalCards) {
        this.totalCards = totalCards;
    }

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public String getTcgdexSetId() {
        return tcgdexSetId;
    }

    public void setTcgdexSetId(String tcgdexSetId) {
        this.tcgdexSetId = tcgdexSetId;
    }

    public String getCopyrightYear() {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear) {
        this.copyrightYear = copyrightYear;
    }

    public String getDexId() {
        return dexId;
    }

    public void setDexId(String dexId) {
        this.dexId = dexId;
    }

    public Integer getNormalizedDexId() {
        return normalizedDexId;
    }

    public void setNormalizedDexId(Integer normalizedDexId) {
        this.normalizedDexId = normalizedDexId;
    }

    public String getExpectedName() {
        return expectedName;
    }

    public void setExpectedName(String expectedName) {
        this.expectedName = expectedName;
    }

    public Boolean getNameMatchesDex() {
        return nameMatchesDex;
    }

    public void setNameMatchesDex(Boolean nameMatchesDex) {
        this.nameMatchesDex = nameMatchesDex;
    }

    public ConfidenceLevel getConfidence() {
        return confidence;
    }

    public void setConfidence(ConfidenceLevel confidence) {
        this.confidence = confidence;
    }

    @Override
    public String toString() {
        return "OcrResult{" +
                "cardName='" + cardName + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", totalCards='" + totalCards + '\'' +
                ", setCode='" + setCode + '\'' +
                ", tcgdexSetId='" + tcgdexSetId + '\'' +
                ", copyrightYear='" + copyrightYear + '\'' +
                ", dexId='" + dexId + '\'' +
                ", normalizedDexId=" + normalizedDexId +
                ", expectedName='" + expectedName + '\'' +
                ", nameMatchesDex=" + nameMatchesDex +
                ", confidence=" + confidence +
                '}';
    }
}