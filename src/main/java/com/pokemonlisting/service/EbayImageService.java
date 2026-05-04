package com.pokemonlisting.service;

import com.pokemonlisting.model.CardImage;
import com.pokemonlisting.model.UploadedImage;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EbayImageService {

    private final CardImageRepository cardImageRepository;
    private final UploadedImageRepository uploadedImageRepository;

    // This base URL is used to build public image URLs that eBay fetches at listing time.
    // Add app.base-url to application.properties (e.g. app.base-url=http://localhost:8080).
    // For sandbox testing: localhost won't work since eBay can't reach it.
    // Use ngrok to expose your local server: run `ngrok http 8080` and paste the https URL here.
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EbayImageService(CardImageRepository cardImageRepository,
                            UploadedImageRepository uploadedImageRepository) {
        this.cardImageRepository = cardImageRepository;
        this.uploadedImageRepository = uploadedImageRepository;
    }

    public List<String> getImageUrls(Long cardId) {
        List<CardImage> cardImages = cardImageRepository.findByCardIdOrderByDisplayOrderAsc(cardId);
        List<String> urls = new ArrayList<>();

        for (CardImage cardImage : cardImages) {
            Optional<UploadedImage> uploadedImage = uploadedImageRepository.findById(cardImage.getUploadedImageId());
            if (uploadedImage.isEmpty()) {
                System.err.println("Warning: no UploadedImage found for CardImage id=" + cardImage.getId());
                continue;
            }
            urls.add(baseUrl + "/api/images/file/" + uploadedImage.get().getSavedFilename());
        }

        return urls;
    }
}
