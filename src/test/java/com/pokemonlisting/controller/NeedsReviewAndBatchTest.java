package com.pokemonlisting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.dto.PokemonCard;
import com.pokemonlisting.model.*;
import com.pokemonlisting.repository.ApiUsageRepository;
import com.pokemonlisting.repository.CardImageRepository;
import com.pokemonlisting.repository.CardRepository;
import com.pokemonlisting.repository.UploadedImageRepository;
import com.pokemonlisting.service.GoogleVisionService;
import com.pokemonlisting.service.Gpt4VisionService;
import com.pokemonlisting.service.PokemonTcgService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class NeedsReviewAndBatchTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private GoogleVisionService googleVisionService;

    @MockitoBean
    private Gpt4VisionService gpt4VisionService;

    @MockitoBean
    private PokemonTcgService pokemonTcgService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardImageRepository cardImageRepository;

    @Autowired
    private UploadedImageRepository uploadedImageRepository;

    @Autowired
    private ApiUsageRepository apiUsageRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        apiUsageRepository.deleteAll();
        cardImageRepository.deleteAll();
        cardRepository.deleteAll();
        uploadedImageRepository.deleteAll();
    }

    //seedNeedsReviewCard(confidence) seeds an IDENTIFIED card with needsReview=true
    //and the given confidence and returns it. Useful for populating the
    //needs-review list without running the full identify pipeline.
    private Card seedNeedsReviewCard(double confidence) {
        Card card = new Card(CardStatus.IDENTIFIED);
        card.setCardName("Pikachu");
        card.setSetName("Scarlet & Violet");
        card.setCardNumber("25");
        card.setConfidence(confidence);
        card.setNeedsReview(true);
        card.setIdentificationMethod("GPT4V");
        return cardRepository.save(card);
    }

    //seedCardWithFrontImage() seeds a Card + UploadedImage + CardImage (FRONT)
    //and returns it. Used by batch identify tests where the pipeline needs
    //a real image path to resolve.
    private Card seedCardWithFrontImage() {
        UploadedImage image = new UploadedImage(
                "test.jpg", "test-uuid.jpg", "/fake/path/test.jpg",
                100L, "image/jpeg", LocalDateTime.now()
        );
        UploadedImage savedImage = uploadedImageRepository.save(image);

        Card card = new Card(CardStatus.FRONT_ONLY);
        Card savedCard = cardRepository.save(card);

        CardImage cardImage = new CardImage(savedCard.getId(), savedImage.getId(), ImageType.FRONT, 1);
        cardImageRepository.save(cardImage);

        return savedCard;
    }

    // --- NEEDS REVIEW TESTS ---

    @Test
    void getNeedsReview_shouldReturnOnlyFlaggedCardsOrderedByConfidence() throws Exception {
        seedNeedsReviewCard(0.3);
        seedNeedsReviewCard(0.5);

        Card notFlagged = new Card(CardStatus.IDENTIFIED);
        notFlagged.setNeedsReview(false);
        cardRepository.save(notFlagged);

        mockMvc.perform(get("/api/cards/needs-review"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].confidence").value(0.3))
                .andExpect(jsonPath("$[1].confidence").value(0.5));
    }

    @Test
    void getNeedsReviewCount_shouldReturnCorrectCount() throws Exception {
        seedNeedsReviewCard(0.3);
        seedNeedsReviewCard(0.5);
        seedNeedsReviewCard(0.6);

        Card notFlagged = new Card(CardStatus.IDENTIFIED);
        notFlagged.setNeedsReview(false);
        cardRepository.save(notFlagged);

        mockMvc.perform(get("/api/cards/needs-review/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void confirmCard_shouldClearNeedsReviewAndSetManual() throws Exception {
        Card card = seedNeedsReviewCard(0.3);

        String body = objectMapper.writeValueAsString(Map.of(
                "cardName", "Charizard",
                "setName", "Base Set",
                "cardNumber", "4",
                "rarity", "Rare Holo"
        ));

        mockMvc.perform(put("/api/cards/" + card.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.needsReview").value(false))
                .andExpect(jsonPath("$.confidence").value(1.0))
                .andExpect(jsonPath("$.identificationMethod").value("MANUAL"))
                .andExpect(jsonPath("$.cardName").value("Charizard"));
    }

    @Test
    void confirmCard_shouldReturn404WhenNotFound() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "cardName", "Charizard",
                "setName", "Base Set",
                "cardNumber", "4",
                "rarity", "Rare Holo"
        ));

        mockMvc.perform(put("/api/cards/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- BATCH IDENTIFY TESTS ---

    @Test
    void identifyBatch_shouldIdentifyAllCards() throws Exception {
        Card card1 = seedCardWithFrontImage();
        Card card2 = seedCardWithFrontImage();

        String ocrText = """
                BASIC
                Tatsugiri
                HP 70
                NO. 0978 Mimicry Pokémon
                H TWMEN 131/167
                ©2024 Pokémon/Nintendo/Creatures/GAME FREAK
                """;

        when(googleVisionService.extractText(anyString())).thenReturn(ocrText);
        when(pokemonTcgService.searchCard(anyString(), anyString()))
                .thenReturn(new PokemonCard("Tatsugiri", "Twilight Masquerade", "131", "Common"));

        String body = objectMapper.writeValueAsString(Map.of("cardIds", List.of(card1.getId(), card2.getId())));

        mockMvc.perform(post("/api/cards/identify-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested").value(2))
                .andExpect(jsonPath("$.identifiedCount").value(2))
                .andExpect(jsonPath("$.googleVisionCount").value(2));
    }

    @Test
    void identifyBatch_shouldSkipAlreadyIdentifiedCards() throws Exception {
        Card frontOnly = seedCardWithFrontImage();

        Card alreadyIdentified = new Card(CardStatus.IDENTIFIED);
        cardRepository.save(alreadyIdentified);

        String ocrText = """
                BASIC
                Tatsugiri
                HP 70
                NO. 0978 Mimicry Pokémon
                H TWMEN 131/167
                ©2024 Pokémon/Nintendo/Creatures/GAME FREAK
                """;

        when(googleVisionService.extractText(anyString())).thenReturn(ocrText);
        when(pokemonTcgService.searchCard(anyString(), anyString()))
                .thenReturn(new PokemonCard("Tatsugiri", "Twilight Masquerade", "131", "Common"));

        String body = objectMapper.writeValueAsString(
                Map.of("cardIds", List.of(frontOnly.getId(), alreadyIdentified.getId()))
        );

        mockMvc.perform(post("/api/cards/identify-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    @Test
    void identifyBatch_shouldReturn400ForEmptyCardIds() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("cardIds", List.of()));

        mockMvc.perform(post("/api/cards/identify-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
