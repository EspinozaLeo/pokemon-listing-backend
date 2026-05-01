package com.pokemonlisting.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class EbayCredentialsConfig {

    @Value("${ebay.credentials.path}")
    private String credentialsPath;

    @Value("${ebay.sandbox}")
    private boolean sandbox;

    private String appId;
    private String devId;
    private String certId;
    private String ruName;
    private String userToken;
    private String accessToken;
    private String refreshToken;

    @PostConstruct
    public void load() throws Exception {
        String content = Files.readString(Path.of(credentialsPath)).trim();
        String[] myStrings = content.split("\n");
        for(int i = 0; i < myStrings.length; i++){
            String[] parts = myStrings[i].split("=", 2);
            if(parts.length < 2) continue;
            String key = parts[0].trim();
            String value = parts[1].trim();
            switch (key) {
                case "app.id"       -> appId = value;
                case "dev.id"       -> devId = value;
                case "cert.id"      -> certId = value;
                case "ru.name"      -> ruName = value;
                case "user.token"   -> userToken = value;
                case "access.token" -> accessToken = value;
                case "refresh.token" -> refreshToken = value;
            }
        }
    }

    public String getAppId()      { return appId; }
    public String getDevId()      { return devId; }
    public String getCertId()     { return certId; }
    public String getRuName()     { return ruName; }
    public String getUserToken()  { return userToken; }
    public String getAccessToken()  { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public boolean isSandbox()    { return sandbox; }

    public void saveTokens(String newAccessToken, String newRefreshToken) throws Exception {
        String content = Files.readString(Path.of(credentialsPath)).trim();
        String[] myStrings = content.split("\n");
        int myStringsLength = myStrings.length;
        boolean accessFound = false;
        boolean refreshFound = false;
        for(int i = 0; i < myStringsLength; i++){
            String[] parts = myStrings[i].split("=", 2);
            if(parts.length < 2) continue;
            String key = parts[0].trim();
            if(key.equals("access.token")){
                myStrings[i] = "access.token=" + newAccessToken;
                accessFound = true;
            }
            if(key.equals("refresh.token")){
                myStrings[i] = "refresh.token=" + newRefreshToken;
                refreshFound = true;
            }
        }
        StringBuilder newString = new StringBuilder(String.join("\n", myStrings));
        if(!accessFound) newString.append("\naccess.token=").append(newAccessToken);
        if(!refreshFound) newString.append("\nrefresh.token=").append(newRefreshToken);
        Files.writeString(Path.of(credentialsPath), newString.toString());
        this.accessToken = newAccessToken;
        this.refreshToken = newRefreshToken;
    }
}
