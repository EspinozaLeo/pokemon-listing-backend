package com.pokemonlisting.controller;

import com.pokemonlisting.dto.*;
import com.pokemonlisting.model.*;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.CardRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import com.pokemonlisting.service.GoogleVisionService;
import com.pokemonlisting.service.Gpt4VisionService;
import com.pokemonlisting.service.OcrParserService;
import com.pokemonlisting.service.PokemonTcgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private static final Logger log = LoggerFactory.getLogger(CardController.class);

    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;
    private final UploadedImageRepository uploadedImageRepository;
    private final GoogleVisionService googleVisionService;
    private final OcrParserService ocrParserService;
    private final PokemonTcgService pokemonTcgService;
    private final Gpt4VisionService gpt4VisionService;

    public CardController(CardRepository cardRepository, CardImageRepository cardImageRepository,
                          UploadedImageRepository uploadedImageRepository, GoogleVisionService googleVisionService,
                          OcrParserService ocrParserService, PokemonTcgService pokemonTcgService,
                          Gpt4VisionService gpt4VisionService) {
        this.cardRepository = cardRepository;
        this.cardImageRepository = cardImageRepository;
        this.uploadedImageRepository = uploadedImageRepository;
        this.googleVisionService = googleVisionService;
        this.ocrParserService = ocrParserService;
        this.pokemonTcgService = pokemonTcgService;
        this.gpt4VisionService = gpt4VisionService;
    }

    //pairImages() pairs all images sequentially. Returns a detailed response
    //of all cards that were paired as well as ids of cards that were not paired
    //due to odd number of cards given.
    //Pairs by FRONT and BACK images.
    //If no images are present, returns response stating failure.
    @PostMapping("/pair")
    public ResponseEntity<PairImagesResponse> pairImages(){
        List<UploadedImage> images = uploadedImageRepository.findAll();
        if(images.isEmpty()){
            PairImagesResponse response = new PairImagesResponse(
                    false,
                    "No images to pair"
            );
            return ResponseEntity.ok(response);
        }

        List<Long> unpairedImgIds = new ArrayList<>();
        List<CardResponse> pairedCards = new ArrayList<>();
        int listSize = images.size();

        //sort by original filename
        images.sort(Comparator.comparing(UploadedImage::getOriginalFilename));

        //loop through the images and create a pair of front and back
        for(int i = 0; i < listSize - 1; i+=2){
            //get the two images for this pair
            UploadedImage imageOne = images.get(i);
            UploadedImage imageTwo = images.get(i+1);

            Card card = new Card(CardStatus.PAIRED);
            Card savedCard = cardRepository.save(card);

            //create card image for front and back and save
            CardImage frontImage = new CardImage(
                    savedCard.getId(),
                    imageOne.getId(),
                    ImageType.FRONT,
                    1
            );
            CardImage savedFrontImage = cardImageRepository.save(frontImage);

            CardImage backImage = new CardImage(
                    savedCard.getId(),
                    imageTwo.getId(),
                    ImageType.BACK,
                    2
            );
            CardImage savedBackImage = cardImageRepository.save(backImage);

            CardImageInfo frontCardImageInfo = new CardImageInfo(
                    savedFrontImage.getId(),
                    savedFrontImage.getUploadedImageId(),
                    imageOne.getOriginalFilename(),
                    savedFrontImage.getImageType(),
                    savedFrontImage.getDisplayOrder()
            );

            CardImageInfo backCardImageInfo = new CardImageInfo(
                    savedBackImage.getId(),
                    savedBackImage.getUploadedImageId(),
                    imageTwo.getOriginalFilename(),
                    savedBackImage.getImageType(),
                    savedBackImage.getDisplayOrder()
            );

            List<CardImageInfo> cardImages = new ArrayList<>();
            cardImages.add(frontCardImageInfo);
            cardImages.add(backCardImageInfo);

            CardResponse cardResponse = new CardResponse(
                    savedCard.getId(),
                    savedCard.getStatus(),
                    cardImages,
                    savedCard.getCreatedAt()
            );

            pairedCards.add((cardResponse));
        }

        if(listSize % 2 != 0 ){
            UploadedImage unpairedImg = images.getLast();
            unpairedImgIds.add(unpairedImg.getId());
        }

        int loopIterations = listSize/2;
        int pairedCount = pairedCards.size();
        int unpairedCount =  listSize - (pairedCount*2);
        PairImagesResponse response = new PairImagesResponse(
                true,
                "Processed " + listSize + " images: " + pairedCount + " paired, " + unpairedCount + " unpaired",
                listSize,
                pairedCount,
                unpairedCount,
                pairedCards,
                unpairedImgIds,
                LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }

    //listAllCards() gets all paired cards.
    //Returns a ResponseEntity containing a list with all paired cards.
    @GetMapping("/list")
    public ResponseEntity<List<CardResponse>> listAllCards(){
        List<Card> cards = cardRepository.findAllByOrderByCreatedAtDesc();
        if(cards.isEmpty()){
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<CardResponse> cardResponses = new ArrayList<>();
        for (Card card : cards){
            List<CardImage> cardImages = cardImageRepository.findByCardIdOrderByDisplayOrderAsc(card.getId());
            List<CardImageInfo> imageInfoList = new ArrayList<>();
            for (CardImage cardImage : cardImages){
                Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(cardImage.getUploadedImageId());
                if(uploadedImageOpt.isPresent()){
                    UploadedImage uploadedImage = uploadedImageOpt.get();
                    CardImageInfo cardImageInfo = new CardImageInfo(
                            cardImage.getId(),
                            cardImage.getUploadedImageId(),
                            uploadedImage.getOriginalFilename(),
                            cardImage.getImageType(),
                            cardImage.getDisplayOrder()
                    );
                    imageInfoList.add(cardImageInfo);
                }
            }
            CardResponse cardResponse = new CardResponse(
                    card.getId(),
                    card.getStatus(),
                    imageInfoList,
                    card.getCreatedAt(),
                    card.getCardName(),
                    card.getSetName(),
                    card.getCardNumber(),
                    card.getRarity(),
                    card.getConfidence(),
                    card.getIdentificationMethod(),
                    card.getNeedsReview(),
                    card.getIdentificationFailureReason()
            );
            cardResponses.add(cardResponse);
        }

        return ResponseEntity.ok(cardResponses);

    }

    //getCardById(id) returns the Card associated with the given id.
    //Returns 404 if not found.
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id){
        Optional<Card> cardOpt = cardRepository.findById(id);

        if(cardOpt.isEmpty()){
            return ResponseEntity.notFound().build();
        }

        Card card = cardOpt.get();

        List<CardImage> cardImages = cardImageRepository.findByCardIdOrderByDisplayOrderAsc(card.getId());
        List<CardImageInfo> imageInfoList = new ArrayList<>();
        for(CardImage cardImage : cardImages){
            Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(cardImage.getUploadedImageId());
            if(uploadedImageOpt.isPresent()){
                UploadedImage uploadedImage = uploadedImageOpt.get();
                CardImageInfo cardImageInfo = new CardImageInfo(
                        cardImage.getId(),
                        cardImage.getUploadedImageId(),
                        uploadedImage.getOriginalFilename(),
                        cardImage.getImageType(),
                        cardImage.getDisplayOrder()
                );
                imageInfoList.add(cardImageInfo);
            }
        }
        CardResponse cardResponse = new CardResponse(
                card.getId(),
                card.getStatus(),
                imageInfoList,
                card.getCreatedAt(),
                card.getCardName(),
                card.getSetName(),
                card.getCardNumber(),
                card.getRarity(),
                card.getConfidence(),
                card.getIdentificationMethod(),
                card.getNeedsReview(),
                card.getIdentificationFailureReason()
        );
        return ResponseEntity.ok(cardResponse);
    }

    //createBulkFrontOnly(request) returns a CardResponse detailing how many
    //Cards were created using front-only images.
    @PostMapping("/create-bulk")
    public ResponseEntity<List<CardResponse>> createBulkFrontOnly(@RequestBody CreateBulkRequest request){
        List<Long> imageIds = request.getImageIds();
        if(imageIds == null || imageIds.isEmpty()){
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<CardResponse> createdCards = new ArrayList<>();
        for(Long imageId : imageIds){
            Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(imageId);
            if(uploadedImageOpt.isEmpty()){
                continue;
            }

            UploadedImage uploadedImage = uploadedImageOpt.get();

            Card card = new Card(CardStatus.FRONT_ONLY);
            Card savedCard = cardRepository.save(card);

            CardImage cardImage = new CardImage(
                    savedCard.getId(),
                    imageId,
                    ImageType.FRONT,
                    1
            );
            CardImage savedFrontImage = cardImageRepository.save(cardImage);

            CardImageInfo frontCardImageInfo = new CardImageInfo(
                    savedFrontImage.getId(),
                    savedFrontImage.getUploadedImageId(),
                    uploadedImage.getOriginalFilename(),
                    savedFrontImage.getImageType(),
                    savedFrontImage.getDisplayOrder()
            );

            List<CardImageInfo> images = new ArrayList<>();
            images.add(frontCardImageInfo);

            CardResponse cardResponse = new CardResponse(
                    savedCard.getId(),
                    savedCard.getStatus(),
                    images,
                    savedCard.getCreatedAt()
            );

            createdCards.add(cardResponse);
        }

        return ResponseEntity.ok(createdCards);
    }

    //createDetailedCard(request) accepts a CreateDetailedCardRequest list
    //of images and returns a CardResponse of a detailed card submission.
    //Must include a FRONT image. Used to create a "high-value" card.
    @PostMapping("/create-detailed")
    public ResponseEntity<CardResponse> createDetailedCard(@RequestBody CreateDetailedCardRequest request){
        List<DetailedImageRequest> images = request.getImages();

        if(images == null || images.isEmpty()){
            return ResponseEntity.badRequest().build();
        }

        boolean hasFront = false;
        boolean hasBack = false;
        for(DetailedImageRequest image : images){
            Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(image.getUploadedImageId());
            if(uploadedImageOpt.isEmpty()){
                return ResponseEntity.badRequest().build();
            }
            if(image.getImageType() == ImageType.FRONT) hasFront = true;
            if(image.getImageType() == ImageType.BACK) hasBack = true;
        }
        if(!hasFront){
            return ResponseEntity.badRequest().build();
        }
        CardStatus status = hasBack ? CardStatus.PAIRED : CardStatus.FRONT_ONLY;

        Card card = new Card(status);
        Card savedCard = cardRepository.save(card);

        List<CardImageInfo> cardImageInfoList = new ArrayList<>();
        for(DetailedImageRequest imageRequest : images){
            Optional<UploadedImage> uploadedImageOptional = uploadedImageRepository.findById(imageRequest.getUploadedImageId());
            UploadedImage uploadedImage = uploadedImageOptional.get();

            CardImage cardImage = new CardImage(
                    savedCard.getId(),
                    imageRequest.getUploadedImageId(),
                    imageRequest.getImageType(),
                    imageRequest.getDisplayOrder()
            );
            CardImage savedCardImage = cardImageRepository.save(cardImage);

            CardImageInfo imageInfo = new CardImageInfo(
                    savedCardImage.getId(),
                    savedCardImage.getUploadedImageId(),
                    uploadedImage.getOriginalFilename(),
                    savedCardImage.getImageType(),
                    savedCardImage.getDisplayOrder()
            );
            cardImageInfoList.add(imageInfo);
        }

        CardResponse cardResponse = new CardResponse(
                savedCard.getId(),
                savedCard.getStatus(),
                cardImageInfoList,
                savedCard.getCreatedAt()
        );

        return ResponseEntity.status(201).body(cardResponse);
    }

    //identifyCard(id) identifies a single card using Google Cloud Vision OCR.
    //Retrieves the FRONT image, runs OCR, parses the result, looks up TCGdex,
    //updates the card fields, and returns the updated CardResponse.
    //Returns 404 if card not found, 400 if card has no FRONT image.
    @PostMapping("/{id}/identify")
    public ResponseEntity<?> identifyCard(@PathVariable Long id) {

        Optional<Card> cardOpt = cardRepository.findById(id);
        if (cardOpt.isEmpty()) return ResponseEntity.notFound().build();
        Card card = cardOpt.get();

        Optional<CardImage> frontImageOpt = cardImageRepository.findByCardIdAndImageType(card.getId(), ImageType.FRONT);
        if (frontImageOpt.isEmpty()) return ResponseEntity.badRequest().build();
        CardImage frontImage = frontImageOpt.get();

        Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(frontImage.getUploadedImageId());
        if (uploadedImageOpt.isEmpty()) return ResponseEntity.badRequest().build();
        String filePath = uploadedImageOpt.get().getFilePath();

        String rawText = googleVisionService.extractText(filePath);
        log.info("[IDENTIFY] Card {} — raw OCR text:\n{}", id, rawText);

        OcrResult ocrResult = ocrParserService.parseCardDetails(rawText);
        log.info("[IDENTIFY] Card {} — parsed: cardName={} cardNumber={} setCode={} tcgdexSetId={} totalCards={} year={} confidence={}",
                id, ocrResult.getCardName(), ocrResult.getCardNumber(), ocrResult.getSetCode(),
                ocrResult.getTcgdexSetId(), ocrResult.getTotalCards(), ocrResult.getCopyrightYear(),
                ocrResult.getConfidence());

        // Track why identification fell back or failed
        String failureReason = null;
        if (rawText == null || rawText.isEmpty()) {
            failureReason = "OCR_NO_TEXT";
        } else if (ocrResult.getCardNumber() == null) {
            failureReason = "OCR_NO_CARD_NUMBER";
        }

        double confidenceScore = switch (ocrResult.getConfidence()) {
            case HIGH -> 0.9;
            case MEDIUM -> 0.7;
            default -> 0.3;
        };

        PokemonCard pokemonCard = null;
        if (confidenceScore >= 0.7
                && ocrResult.getTcgdexSetId() != null
                && ocrResult.getCardNumber() != null) {
            log.info("[IDENTIFY] Card {} — calling TCGdex: cardNumber={} setId={}", id, ocrResult.getCardNumber(), ocrResult.getTcgdexSetId());
            pokemonCard = pokemonTcgService.searchCard(ocrResult.getCardNumber(), ocrResult.getTcgdexSetId());
            log.info("[IDENTIFY] Card {} — TCGdex result: {}", id, pokemonCard != null ? pokemonCard.getName() + " / " + pokemonCard.getSetName() : "null");
            if (pokemonCard == null && failureReason == null) {
                failureReason = "TCGDEX_NO_MATCH";
            }
        }

        if (confidenceScore < 0.7 || ocrResult.getCardNumber() == null || pokemonCard == null) {
            log.info("[IDENTIFY] Card {} — falling back to GPT4V (confidence={} cardNumber={} pokemonCard={})",
                    id, confidenceScore, ocrResult.getCardNumber(), pokemonCard);
            Gpt4VisionService.CardData gptResult = gpt4VisionService.identifyCard(filePath);
            if (gptResult != null) {
                log.info("[IDENTIFY] Card {} — GPT4V result: {} / {}", id, gptResult.getCardName(), gptResult.getCardNumber());
                card.setCardName(gptResult.getCardName());
                card.setSetName(gptResult.getSetName());
                card.setCardNumber(gptResult.getCardNumber());
                card.setRarity(gptResult.getRarity());
                card.setConfidence(0.8);
                card.setIdentificationMethod("GPT4V");
                card.setNeedsReview(true);
                card.setIdentificationFailureReason("GPT_FALLBACK_USED");
                card.setStatus(CardStatus.IDENTIFIED);
                Card savedCard = cardRepository.save(card);
                return ResponseEntity.ok(buildCardResponse(savedCard));
            }
            log.warn("[IDENTIFY] Card {} — GPT4V returned null, saving partial OCR data", id);
            failureReason = "GPT_PARSE_FAILED";
        }

        card.setCardName(pokemonCard != null ? pokemonCard.getName() : ocrResult.getCardName());
        card.setSetName(pokemonCard != null ? pokemonCard.getSetName() : ocrResult.getTcgdexSetId());
        card.setCardNumber(ocrResult.getCardNumber());
        card.setRarity(pokemonCard != null ? pokemonCard.getRarity() : null);
        card.setConfidence(confidenceScore);
        card.setIdentificationMethod("GOOGLE_VISION");
        card.setNeedsReview(confidenceScore < 0.7);
        // Clear failure reason if Google Vision identified successfully with high confidence
        card.setIdentificationFailureReason(confidenceScore >= 0.7 ? null : failureReason);
        card.setStatus(CardStatus.IDENTIFIED);

        Card savedCard = cardRepository.save(card);
        return ResponseEntity.ok(buildCardResponse(savedCard));
    }

    //getNeedsReview() returns all cards flagged for manual review,
    //ordered by confidence ascending (least confident first).
    //Returns an empty array if no cards need review.
    @GetMapping("/needs-review")
    public ResponseEntity<List<CardResponse>> getNeedsReview() {
        List<Card> cards = cardRepository.findByNeedsReviewTrueOrderByConfidenceAsc();
        List<CardResponse> cardResponses = new ArrayList<>();
        for (Card card : cards) {
            cardResponses.add(buildCardResponse(card));
        }
        return ResponseEntity.ok(cardResponses);
    }

    //getNeedsReviewCount() returns the total number of cards flagged for review.
    //Useful for displaying a badge count on the frontend.
    @GetMapping("/needs-review/count")
    public ResponseEntity<Long> getNeedsReviewCount() {
        return ResponseEntity.ok(cardRepository.countByNeedsReviewTrue());
    }

    //confirmCard(id, request) lets the user confirm or correct a card's details.
    //Sets needsReview=false, confidence=1.0, identificationMethod="MANUAL".
    //Returns 404 if card not found.
    @PutMapping("/{id}")
    public ResponseEntity<CardResponse> confirmCard(@PathVariable Long id,
                                                    @RequestBody ConfirmCardRequest request) {
        Optional<Card> cardOpt = cardRepository.findById(id);
        if (cardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Card card = cardOpt.get();
        card.setCardName(request.getCardName());
        card.setSetName(request.getSetName());
        card.setCardNumber(request.getCardNumber());
        card.setRarity(request.getRarity());
        card.setNeedsReview(false);
        card.setConfidence(1.0);
        card.setIdentificationMethod("MANUAL");

        Card savedCard = cardRepository.save(card);
        return ResponseEntity.ok(buildCardResponse(savedCard));
    }

    //identifyBatch(request) identifies multiple cards in one request using the
    //same pipeline as identifyCard(). Processes sequentially with a 100ms delay
    //between calls to avoid rate limits. Continues if one card fails.
    //Returns 400 if cardIds is null or empty.
    @PostMapping("/identify-batch")
    public ResponseEntity<BatchIdentifyResponse> identifyBatch(@RequestBody BatchIdentifyRequest request) {
        List<Long> cardIds = request.getCardIds();
        if (cardIds == null || cardIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<BatchCardResult> results = new ArrayList<>();
        int identifiedCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        int googleVisionCount = 0;
        int gpt4vCount = 0;

        for (Long cardId : cardIds) {
            Optional<Card> cardOpt = cardRepository.findById(cardId);
            if (cardOpt.isEmpty()) {
                results.add(new BatchCardResult(cardId, false, "Card not found"));
                skippedCount++;
                continue;
            }
            Card card = cardOpt.get();

            if (card.getStatus() == CardStatus.IDENTIFIED) {
                results.add(new BatchCardResult(cardId, false, card.getCardName(),
                        card.getCardNumber(), card.getConfidence(), card.getIdentificationMethod(),
                        "Already identified", card.getIdentificationFailureReason()));
                skippedCount++;
                continue;
            }

            Optional<CardImage> frontImageOpt = cardImageRepository.findByCardIdAndImageType(card.getId(), ImageType.FRONT);
            if (frontImageOpt.isEmpty()) {
                results.add(new BatchCardResult(cardId, false, "No FRONT image"));
                skippedCount++;
                continue;
            }

            Optional<UploadedImage> uploadedImageOpt = uploadedImageRepository.findById(frontImageOpt.get().getUploadedImageId());
            if (uploadedImageOpt.isEmpty()) {
                results.add(new BatchCardResult(cardId, false, "Uploaded image not found"));
                skippedCount++;
                continue;
            }

            try {
                String filePath = uploadedImageOpt.get().getFilePath();
                String rawText = googleVisionService.extractText(filePath);
                OcrResult ocrResult = ocrParserService.parseCardDetails(rawText);

                String failureReason = null;
                if (rawText == null || rawText.isEmpty()) {
                    failureReason = "OCR_NO_TEXT";
                } else if (ocrResult.getCardNumber() == null) {
                    failureReason = "OCR_NO_CARD_NUMBER";
                }

                double confidenceScore = switch (ocrResult.getConfidence()) {
                    case HIGH -> 0.9;
                    case MEDIUM -> 0.7;
                    default -> 0.3;
                };

                PokemonCard pokemonCard = null;
                if (confidenceScore >= 0.7
                        && ocrResult.getTcgdexSetId() != null
                        && ocrResult.getCardNumber() != null) {
                    pokemonCard = pokemonTcgService.searchCard(ocrResult.getCardNumber(), ocrResult.getTcgdexSetId());
                    if (pokemonCard == null && failureReason == null) {
                        failureReason = "TCGDEX_NO_MATCH";
                    }
                }

                if (confidenceScore < 0.7 || ocrResult.getCardNumber() == null || pokemonCard == null) {
                    Gpt4VisionService.CardData gptResult = gpt4VisionService.identifyCard(filePath);
                    if (gptResult != null) {
                        card.setCardName(gptResult.getCardName());
                        card.setSetName(gptResult.getSetName());
                        card.setCardNumber(gptResult.getCardNumber());
                        card.setRarity(gptResult.getRarity());
                        card.setConfidence(0.8);
                        card.setIdentificationMethod("GPT4V");
                        card.setNeedsReview(true);
                        card.setIdentificationFailureReason("GPT_FALLBACK_USED");
                        card.setStatus(CardStatus.IDENTIFIED);
                        cardRepository.save(card);
                        results.add(new BatchCardResult(cardId, true, card.getCardName(),
                                card.getCardNumber(), 0.8, "GPT4V", "GPT_FALLBACK_USED"));
                        identifiedCount++;
                        gpt4vCount++;
                        TimeUnit.MILLISECONDS.sleep(100);
                        continue;
                    }
                    failureReason = "GPT_PARSE_FAILED";
                }

                card.setCardName(pokemonCard != null ? pokemonCard.getName() : ocrResult.getCardName());
                card.setSetName(pokemonCard != null ? pokemonCard.getSetName() : ocrResult.getTcgdexSetId());
                card.setCardNumber(ocrResult.getCardNumber());
                card.setRarity(pokemonCard != null ? pokemonCard.getRarity() : null);
                card.setConfidence(confidenceScore);
                card.setIdentificationMethod("GOOGLE_VISION");
                card.setNeedsReview(confidenceScore < 0.7);
                card.setIdentificationFailureReason(confidenceScore >= 0.7 ? null : failureReason);
                card.setStatus(CardStatus.IDENTIFIED);
                cardRepository.save(card);

                results.add(new BatchCardResult(cardId, true, card.getCardName(),
                        card.getCardNumber(), confidenceScore, "GOOGLE_VISION",
                        confidenceScore >= 0.7 ? null : failureReason));
                identifiedCount++;
                googleVisionCount++;

            } catch (Exception e) {
                results.add(new BatchCardResult(cardId, false, e.getMessage()));
                failedCount++;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        BatchIdentifyResponse response = new BatchIdentifyResponse(
                cardIds.size(), identifiedCount, failedCount, skippedCount,
                googleVisionCount, gpt4vCount, results
        );
        return ResponseEntity.ok(response);
    }

    //buildCardResponse(card) builds a full CardResponse for a saved Card,
    //including all linked images and identification fields.
    private CardResponse buildCardResponse(Card card) {
        List<CardImage> cardImages = cardImageRepository.findByCardIdOrderByDisplayOrderAsc(card.getId());
        List<CardImageInfo> imageInfoList = new ArrayList<>();
        for (CardImage cardImage : cardImages) {
            Optional<UploadedImage> imgOpt = uploadedImageRepository.findById(cardImage.getUploadedImageId());
            if (imgOpt.isPresent()) {
                CardImageInfo imageInfo = new CardImageInfo(
                        cardImage.getId(),
                        cardImage.getUploadedImageId(),
                        imgOpt.get().getOriginalFilename(),
                        cardImage.getImageType(),
                        cardImage.getDisplayOrder()
                );
                imageInfoList.add(imageInfo);
            }
        }
        return new CardResponse(
                card.getId(),
                card.getStatus(),
                imageInfoList,
                card.getCreatedAt(),
                card.getCardName(),
                card.getSetName(),
                card.getCardNumber(),
                card.getRarity(),
                card.getConfidence(),
                card.getIdentificationMethod(),
                card.getNeedsReview(),
                card.getIdentificationFailureReason()
        );
    }
}
