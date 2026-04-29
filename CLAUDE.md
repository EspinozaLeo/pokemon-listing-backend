# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=OcrParserServiceTest

# Build without running tests
./mvnw package -DskipTests

# Clean build
./mvnw clean package -DskipTests
```

## Architecture

This is a Spring Boot REST API for automated Pokemon card eBay listing creation. Java 21, PostgreSQL 16, Maven.

**Core pipeline:** Image uploaded → Google Cloud Vision OCR → `OcrParserService` parses raw text → `PokemonTcgService` looks up card in TCGdex → `Card` entity updated with identification data → GPT-4V fallback if confidence low.

### Database

Three tables managed by `ddl-auto=update` (Hibernate auto-creates/updates schema on startup — no migration files):
- `uploaded_images` — raw uploaded files, one row per file
- `cards` — a card record, holds identification fields (cardName, setName, cardNumber, rarity, confidence, identificationMethod, needsReview)
- `card_images` — junction table linking cards to uploaded images, stores `ImageType` (FRONT, BACK, CLOSEUP, EDGE, SURFACE, DAMAGE) and `displayOrder`

`CardImage` uses plain `Long cardId` and `Long uploadedImageId` foreign keys — **not** JPA `@ManyToOne` relationships. Keep it this way.

### Key Design Decisions

**CardResponse constructor overloading:** There are two constructors — a 4-param backward-compatible one (used by pair/list/getById) and an 11-param one (used by identify). The 4-param delegates to the 11-param with nulls. Always use the 11-param constructor in new endpoints.

**Set code identification strategy in `OcrParserService`/`PokemonSetCodeMapper`:**
1. Try `codeToTcgdexId` — only Scarlet & Violet era cards print set codes on card fronts
2. Fall back to `totalCardsMapping` — keyed by `"totalCards:copyrightYear"` (e.g. `"198:2023"` → `sv01`) — covers Sword & Shield and older sets
3. Ambiguous codes (PAL/PAR/PAF) are handled by the total+year fallback, not by code lookup

**Confidence scoring:** `HIGH=0.9`, `MEDIUM=0.7`, `LOW=0.3`. Cards with `confidence < 0.7` get `needsReview=true`. GPT-4V fallback triggers when confidence < 0.7, card number is null, or TCGdex returns no match. GPT-4V results use `confidence=0.8` and `identificationMethod="GPT4V"`.

**`TesseractOcrService`** exists but is NOT wired as a `@Service` bean and is not used in the active pipeline. `GoogleVisionService` is the active OCR implementation.

### External APIs

- **Google Cloud Vision** — credentials loaded from path in `application.properties` (`google.vision.credentials.path`) or `GOOGLE_APPLICATION_CREDENTIALS` env var. Credentials file at `src/main/resources/credentials/` is gitignored.
- **TCGdex** (`https://api.tcgdex.net/v2/en`) — free, no API key. Used by `PokemonTcgService.searchCard(cardNumber, setId)`.
- **OpenAI GPT-4V** — key stored in `application.properties` as `openai.api.key`. Used by `Gpt4VisionService` as fallback.

### Endpoint Summary

| Method | Path | Description |
|---|---|---|
| POST | `/api/upload/single` | Upload one image |
| POST | `/api/upload/multiple` | Upload multiple images |
| GET | `/api/images/list` | List all uploaded images |
| GET | `/api/images/count` | Count uploaded images |
| DELETE | `/api/images/{id}` | Delete image from DB + disk |
| POST | `/api/cards/pair` | Pair all uploaded images sequentially (FRONT/BACK) |
| POST | `/api/cards/create-bulk` | Create FRONT_ONLY cards from list of image IDs |
| POST | `/api/cards/create-detailed` | Create card with explicit image type assignments |
| GET | `/api/cards/list` | List all cards with identification fields |
| GET | `/api/cards/{id}` | Get single card |
| POST | `/api/cards/{id}/identify` | Run OCR + TCGdex + GPT-4V fallback on card |
| POST | `/api/cards/{id}/list` | List a single identified card on eBay sandbox |

### eBay Integration

- **Credentials** — loaded from `src/main/resources/credentials/ebay-sandbox-credentials.txt` (gitignored) via `EbayCredentialsConfig`. Properties: `app.id`, `dev.id`, `cert.id`, `ru.name`, `user.token`.
- **`EbayTokenService`** — wraps credentials; provides `getBearerToken()`, `getBaseUrl()`, `getMarketplaceId()`.
- **`EbayListingService`** — full listing flow: create inventory item (PUT) → create offer (POST) → publish offer (POST). SKU format: `"CARD-{cardId}"`. Only lists cards with `CardStatus.IDENTIFIED`.
- **Policy IDs** — `EbayListingService.createAndPublishOffer()` has placeholder policy IDs (`REPLACE_WITH_*`) that must be replaced with real sandbox seller account policy IDs before listing will work.
- **Token expiry** — eBay OAuth access tokens expire in ~2 hours. Must be manually regenerated from the eBay developer portal for now (TLS-46/47/48 will add auto-refresh).
