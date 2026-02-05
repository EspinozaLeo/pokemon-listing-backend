package com.pokemonlisting.controller;

import com.pokemonlisting.dto.CardImageInfo;
import com.pokemonlisting.dto.CardResponse;
import com.pokemonlisting.dto.CreateBulkRequest;
import com.pokemonlisting.dto.PairImagesResponse;
import com.pokemonlisting.model.*;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.CardRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;
    private final UploadedImageRepository uploadedImageRepository;

    public CardController(CardRepository cardRepository, CardImageRepository cardImageRepository, UploadedImageRepository uploadedImageRepository) {
        this.cardRepository = cardRepository;
        this.cardImageRepository = cardImageRepository;
        this.uploadedImageRepository = uploadedImageRepository;
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
                    card.getCreatedAt()
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
                card.getCreatedAt()
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
}
