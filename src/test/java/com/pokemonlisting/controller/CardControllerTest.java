package com.pokemonlisting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemonlisting.model.Card;
import com.pokemonlisting.model.CardStatus;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class CardControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mock external services — no real API calls in tests
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

        // Wipe DB before each test so tests don't bleed state into each other
        apiUsageRepository.deleteAll();
        cardImageRepository.deleteAll();
        cardRepository.deleteAll();
        uploadedImageRepository.deleteAll();
    }

    // TODO 3 — POST /api/upload/single
    // Hint: use MockMultipartFile to simulate a file upload
    // MockMultipartFile takes: field name ("file"), original filename, content type, and byte content
    // Use mockMvc.perform(multipart("/api/upload/single").file(mockFile))
    // Assert: status 200, response contains the original filename
    @Test
    void uploadSingleImage_shouldReturn200AndSaveFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/upload/single").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("test.jpg"));

        assertEquals(1, uploadedImageRepository.count());
    }

    // TODO 4 — POST /api/cards/pair
    // Hint: upload 2 images first using the helper above, then call pair
    // Use mockMvc.perform(post("/api/cards/pair"))
    // Assert: status 200, "pairedCards" has 1 entry, card has FRONT and BACK images
    @Test
    void pairImages_shouldCreateOneCardWithFrontAndBack() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "aaa.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile file2 = new MockMultipartFile("file", "bbb.jpg", "image/jpeg", new byte[]{4, 5, 6});

        mockMvc.perform(multipart("/api/upload/single").file(file1));
        mockMvc.perform(multipart("/api/upload/single").file(file2));

        mockMvc.perform(post("/api/cards/pair"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pairedCards.length()").value(1));

        assertEquals(1, cardRepository.count());
        assertEquals(2, cardImageRepository.count());
    }

    // TODO 5 — POST /api/cards/create-bulk
    // Hint: upload 2 images, extract their IDs from the upload response,
    // then POST {"imageIds": [id1, id2]} to /api/cards/create-bulk
    // Use objectMapper.writeValueAsString(Map.of("imageIds", List.of(id1, id2))) for the body
    @Test
    void createBulk_shouldCreateFrontOnlyCards() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("file", "card1.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MockMultipartFile file2 = new MockMultipartFile("file", "card2.jpg", "image/jpeg", new byte[]{4, 5, 6});

        MvcResult result1 = mockMvc.perform(multipart("/api/upload/single").file(file1)).andReturn();
        MvcResult result2 = mockMvc.perform(multipart("/api/upload/single").file(file2)).andReturn();

        long id1 = objectMapper.readTree(result1.getResponse().getContentAsString()).get("id").asLong();
        long id2 = objectMapper.readTree(result2.getResponse().getContentAsString()).get("id").asLong();

        String body = objectMapper.writeValueAsString(Map.of("imageIds", List.of(id1, id2)));

        mockMvc.perform(post("/api/cards/create-bulk").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("FRONT_ONLY"))
                .andExpect(jsonPath("$[1].status").value("FRONT_ONLY"));

        assertEquals(2, cardRepository.count());
    }

    // TODO 6 — GET /api/cards/list
    // Hint: directly save a Card to cardRepository (no upload needed — seed the DB directly)
    // Use: Card card = new Card(CardStatus.FRONT_ONLY); cardRepository.save(card);
    @Test
    void listCards_shouldReturnAllCards() throws Exception {
        cardRepository.save(new Card(CardStatus.FRONT_ONLY));
        cardRepository.save(new Card(CardStatus.FRONT_ONLY));

        mockMvc.perform(get("/api/cards/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // TODO 7a — GET /api/cards/{id} — happy path
    // Hint: seed a card, capture its ID, then GET /api/cards/{id}
    @Test
    void getCardById_shouldReturnCorrectCard() throws Exception {
        Card saved = cardRepository.save(new Card(CardStatus.FRONT_ONLY));

        mockMvc.perform(get("/api/cards/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(saved.getId()));
    }

    // TODO 7b — GET /api/cards/{id} — not found
    @Test
    void getCardById_shouldReturn404WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/cards/99999"))
                .andExpect(status().isNotFound());
    }
}
