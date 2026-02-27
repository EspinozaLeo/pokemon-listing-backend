# Google Cloud Vision Credentials Setup

This directory contains Google Cloud Vision API credentials for OCR text extraction from Pokemon card images.

## Setup Instructions

### 1. Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing project
3. Enable **Cloud Vision API**
    - Search for "Vision API" in the search bar
    - Click "Enable"

### 2. Create Service Account & Credentials
1. Navigate to **IAM & Admin** → **Service Accounts**
2. Click **"+ Create Service Account"**
3. Fill in details:
    - Name: `pokemon-card-ocr`
    - Description: `OCR service for Pokemon card identification`
4. Grant permissions:
    - Role: **Cloud Vision AI Service Agent** (or Owner for development)
5. Click **"Done"**
6. Find your service account → Click **⋮** (three dots) → **Manage keys**
7. Click **"Add Key"** → **"Create new key"**
8. Select **JSON** format
9. Click **"Create"** (file downloads automatically)

### 3. Configure Credentials

You have **two options** for providing credentials:

#### Option A: Environment Variable (Recommended)

**Windows PowerShell:**
```powershell
# Temporary (current session only)
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\your\credentials.json"

# Permanent (set in System Environment Variables)
# 1. Search Windows: "Environment Variables"
# 2. Click "Environment Variables" button
# 3. Under "User variables", click "New"
# 4. Variable name: GOOGLE_APPLICATION_CREDENTIALS
# 5. Variable value: C:\path\to\your\credentials.json
# 6. Click OK and restart IntelliJ
```

**Linux/Mac:**
```bash
# Add to ~/.bashrc or ~/.zshrc
export GOOGLE_APPLICATION_CREDENTIALS="/path/to/your/credentials.json"

# Apply changes
source ~/.bashrc
```

#### Option B: Application Properties (Development)

1. Save your downloaded JSON file to this directory:
```
   src/main/resources/credentials/YOUR-FILE-NAME.json
```

2. Update `src/main/resources/application.properties`:
```properties
   google.vision.credentials.path=src/main/resources/credentials/YOUR-FILE-NAME.json
```

**Note:** Replace `YOUR-FILE-NAME.json` with your actual filename (e.g., `pokemon-card-listing-6e6478f01699.json`)

**Priority:** The service will check for credentials in this order:
1. Environment variable `GOOGLE_APPLICATION_CREDENTIALS` (if set)
2. `application.properties` value (fallback)

## Security

### ⚠️ CRITICAL SECURITY RULES

- **NEVER commit credentials to Git!**
- **NEVER share credentials publicly**
- **NEVER hardcode credentials in source code**

### How We Protect Credentials

- ✅ This directory is in `.gitignore`
- ✅ All `*.json` files in this directory are ignored by Git
- ✅ Only `README.md` is tracked in version control
- ✅ Credentials are loaded at runtime from secure location
- ✅ Service uses Spring's `@Value` injection for configuration

### If Credentials Are Accidentally Committed

1. **Immediately revoke the credentials** in Google Cloud Console
2. Generate new credentials
3. Remove from Git history:
```bash
   git filter-branch --force --index-filter \
     "git rm --cached --ignore-unmatch src/main/resources/credentials/*.json" \
     --prune-empty --tag-name-filter cat -- --all
```
4. Force push (coordinate with team first!)

## Testing

### Run Unit Tests

Tests require valid credentials and use Spring Boot test context.

**Prerequisites:**
1. Configure credentials using Option A or Option B above
2. Ensure test image exists in `src/test/resources/test-images/sample-card.jpg`

**Running Tests:**
```bash
# Run all tests
mvn test

# Run only GoogleVisionServiceTest
mvn test -Dtest=GoogleVisionServiceTest
```

**Test Configuration:**
- Tests use `@SpringBootTest` annotation
- Service is auto-injected via `@Autowired`
- Test image is loaded from `src/test/resources/test-images/sample-card.jpg`
- Tests properly handle spaces in directory paths

### Adding Your Own Test Images

1. Place card images in: `src/test/resources/test-images/`
2. Name them descriptively (e.g., `charizard-base-set.jpg`)
3. Update test code to reference your image
4. Commit test images with code (they're small, ~50-200KB each)

## Developer Setup Guide

### First-Time Setup

1. **Get credentials:**
    - Follow setup instructions above
    - Download JSON credentials file

2. **Choose configuration method:**

   **Option A (Recommended):** Set environment variable
```powershell
   $env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\credentials.json"
```
Then restart IntelliJ

**Option B:** Update `application.properties`
```properties
   google.vision.credentials.path=src/main/resources/credentials/YOUR-FILE.json
```

3. **Add test image:**
    - Copy a Pokemon card image to `src/test/resources/test-images/sample-card.jpg`
    - Or use existing test image if already present

4. **Run tests:**
```bash
   mvn test
```

5. **Verify everything works:**
    - All tests should pass ✅
    - Console should show extracted OCR text

## Cost Information

### Free Tier
- **First 1,000 images/month: FREE** ✅
- Perfect for development and testing

### Paid Tier
- **After 1,000 images: $1.50 per 1,000 images**
- Example costs:
    - 100 cards/month: **$0.00** (under free tier)
    - 5,000 cards/month: **$6.00**
    - 10,000 cards/month: **$13.50**

### Cost Monitoring
- View usage in [Google Cloud Console](https://console.cloud.google.com/billing)
- Set up budget alerts to avoid surprises
- Track API calls in application logs (coming in TLS-24)

## Troubleshooting

### Error: "Could not find default credentials"

**Cause:** Credentials not configured

**Solutions:**
1. Verify credentials file exists in this directory
2. Check environment variable: `echo $env:GOOGLE_APPLICATION_CREDENTIALS` (Windows) or `echo $GOOGLE_APPLICATION_CREDENTIALS` (Linux/Mac)
3. **Restart IntelliJ** after setting environment variable (critical!)
4. Verify `application.properties` has correct path
5. Check for typos in file path

### Error: "Failed to load Google Cloud credentials"

**Cause:** Invalid credentials path or malformed JSON

**Solutions:**
1. Verify file path is correct
2. Check JSON file is valid (not corrupted)
3. Ensure file is readable (check permissions)
4. Try downloading credentials again from Google Cloud Console

### Error: "Permission denied" or "PERMISSION_DENIED"

**Cause:** Service account doesn't have Vision API permissions

**Solutions:**
1. Go to Google Cloud Console → IAM & Admin
2. Find your service account
3. Add role: **Cloud Vision AI Service Agent**
4. Wait 2-3 minutes for permissions to propagate

### Error: "API not enabled" or "Cloud Vision API has not been used"

**Cause:** Cloud Vision API not enabled for your project

**Solutions:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Search for "Vision API"
3. Click "Enable"
4. Wait 2-3 minutes for propagation
5. Try again

### Tests Failing with "Test image not found"

**Cause:** Test image missing from resources

**Solutions:**
1. Verify file exists: `src/test/resources/test-images/sample-card.jpg`
2. Right-click `test-images` folder in IntelliJ → Reload from Disk
3. Check file name is exactly `sample-card.jpg` (case-sensitive on Linux/Mac)
4. Rebuild project: Build → Rebuild Project

### Tests Failing with "File not found" (path with %20)

**Cause:** Spaces in directory path not properly handled

**Solution:** Already fixed in code! Test uses `resource.toURI()` which properly handles URL encoding.

### General Test Failures

**Solutions:**
1. Verify credentials are valid (not expired/revoked)
2. Check network connectivity to Google Cloud
3. Verify API quota not exceeded (check Google Cloud Console)
4. Restart IntelliJ if environment variables recently set
5. Clean and rebuild: `mvn clean install`

## Additional Resources

- [Google Cloud Vision Documentation](https://cloud.google.com/vision/docs)
- [Java Client Library Reference](https://cloud.google.com/java/docs/reference/google-cloud-vision/latest)
- [OCR Best Practices](https://cloud.google.com/vision/docs/ocr)
- [Pricing Calculator](https://cloud.google.com/vision/pricing)
- [Authentication Guide](https://cloud.google.com/docs/authentication/getting-started)
- [Spring Boot @Value Annotation](https://www.baeldung.com/spring-value-annotation)

## Support

For issues with:
- **Credentials setup:** See Google Cloud Vision documentation
- **Application code:** Check `GoogleVisionService.java` and tests
- **Cost concerns:** Monitor usage in Google Cloud Console
- **Test failures:** See Troubleshooting section above

---

**Last Updated:** February 2026  
**Maintained By:** Development Team