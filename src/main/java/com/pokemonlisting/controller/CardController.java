package com.pokemonlisting.controller;

import com.pokemonlisting.dto.CardImageInfo;
import com.pokemonlisting.dto.CardResponse;
import com.pokemonlisting.dto.PairImagesResponse;
import com.pokemonlisting.model.*;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.CardRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
