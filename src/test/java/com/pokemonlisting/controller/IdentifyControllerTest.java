package com.pokemonlisting.controller;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class IdentifyControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

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

    //seedCardWithFrontImage() saves a Card + UploadedImage + CardImage (FRONT) to the DB so identify has something to work with.
    //Returns the saved Card.
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

    // TODO 1 — Happy path: Google Vision → HIGH confidence → TCGdex match → IDENTIFIED
    // Setup:
    //   - googleVisionService.extractText(anyString()) returns OCR text with a card number and set code
    //     that will parse to HIGH confidence (use a real card string like the Eevee text from OcrParserServiceTest)
    //   - pokemonTcgService.searchCard(anyString(), anyString()) returns a PokemonCard (construct one directly)
    //   - gpt4VisionService is NOT called (you can verify this with Mockito.verify if you want)
    // Assert:
    //   - status 200
    //   - response JSON "identificationMethod" = "GOOGLE_VISION"
    //   - response JSON "needsReview" = false
    //   - response JSON "cardName" matches the PokemonCard name you returned from the mock
    //   - cardRepository.findById(card.getId()).get().getStatus() == CardStatus.IDENTIFIED
    @Test
    void identify_happyPath_googleVisionAndTcgdex() throws Exception {
        Card card = seedCardWithFrontImage();

        String ocrText = """
                BASIC
                Tatsugiri
                00
                NO. 0978 Mimicry Pokémon HT: 1 WT 17.6 lbs.
                HP
                70
                Surf
                50
                weakness
                resistance
                retreat
                H TWMEN 131/167
                ©2024 Pokémon/Nintendo/Creatures/GAME FREAK
                """;

        when(googleVisionService.extractText(anyString())).thenReturn(ocrText);
        when(pokemonTcgService.searchCard(anyString(), anyString()))
                .thenReturn(new PokemonCard("Tatsugiri", "Twilight Masquerade", "131", "Common"));

        mockMvc.perform(post("/api/cards/" + card.getId() + "/identify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificationMethod").value("GOOGLE_VISION"))
                .andExpect(jsonPath("$.needsReview").value(false))
                .andExpect(jsonPath("$.cardName").value("Tatsugiri"));

        assertEquals(CardStatus.IDENTIFIED, cardRepository.findById(card.getId()).get().getStatus());
    }

    // TODO 2 — GPT4V fallback: OCR returns empty text → confidence LOW → GPT4V called → IDENTIFIED
    // Setup:
    //   - googleVisionService.extractText returns "" (empty string triggers OCR_NO_TEXT path)
    //   - gpt4VisionService.identifyCard returns a Gpt4VisionService.CardData
    //     Hint: new Gpt4VisionService.CardData("Pikachu", "sv01", "25", "Common")
    //   - pokemonTcgService is NOT called
    // Assert:
    //   - status 200
    //   - response JSON "identificationMethod" = "GPT4V"
    //   - response JSON "needsReview" = true
    //   - response JSON "identificationFailureReason" = "GPT_FALLBACK_USED"
    @Test
    void identify_lowConfidence_fallsBackToGpt4v() throws Exception {
        Card card = seedCardWithFrontImage();

        when(googleVisionService.extractText(anyString())).thenReturn("");
        when(gpt4VisionService.identifyCard(anyString()))
                .thenReturn(new Gpt4VisionService.CardData("Pikachu", "Scarlet & Violet", "25", "Common"));

        mockMvc.perform(post("/api/cards/" + card.getId() + "/identify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificationMethod").value("GPT4V"))
                .andExpect(jsonPath("$.needsReview").value(true))
                .andExpect(jsonPath("$.identificationFailureReason").value("GPT_FALLBACK_USED"));
    }

    @Test
    void identify_gpt4vReturnsNull_savesWithFailureReason() throws Exception {
        Card card = seedCardWithFrontImage();

        when(googleVisionService.extractText(anyString())).thenReturn("");
        when(gpt4VisionService.identifyCard(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/cards/" + card.getId() + "/identify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identificationFailureReason").value("GPT_PARSE_FAILED"));
    }
    
    @Test
    void identify_cardNotFound_returns404() throws Exception {
        mockMvc.perform(post("/api/cards/99999/identify"))
                .andExpect(status().isNotFound());
    }

    // TODO 5 — No FRONT image: card exists but has no CardImage with ImageType.FRONT → 400
    // Hint: save a Card directly to cardRepository WITHOUT calling seedCardWithFrontImage()
    @Test
    void identify_noFrontImage_returns400() throws Exception {
        Card card = cardRepository.save(new Card(CardStatus.FRONT_ONLY));

        mockMvc.perform(post("/api/cards/" + card.getId() + "/identify"))
                .andExpect(status().isBadRequest());
    }
}
