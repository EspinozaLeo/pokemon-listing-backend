package com.pokemonlisting.dto;

public class ListCardResponse {

    private Long cardId;
    private String cardName;
    private String ebayListingId;
    private String ebayListingUrl;
    private String status;
    private String errorReason;
    private boolean imagesAttached;

    public ListCardResponse(Long cardId, String cardName, String ebayListingId, String ebayListingUrl, boolean imagesAttached) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.ebayListingId = ebayListingId;
        this.ebayListingUrl = ebayListingUrl;
        this.status = "LISTED";
        this.imagesAttached = imagesAttached;
    }

    public ListCardResponse(Long cardId, String cardName, String errorReason, boolean failed) {
        this.cardId = cardId;
        this.cardName = cardName;
        this.errorReason = errorReason;
        this.status = "FAILED";
    }

    public Long getCardId() { return cardId; }
    public String getCardName() { return cardName; }
    public String getEbayListingId() { return ebayListingId; }
    public String getEbayListingUrl() { return ebayListingUrl; }
    public String getStatus() { return status; }
    public String getErrorReason() { return errorReason; }
    public boolean isImagesAttached() { return imagesAttached; }
    public void setImagesAttached(boolean imagesAttached) { this.imagesAttached = imagesAttached; }
}
