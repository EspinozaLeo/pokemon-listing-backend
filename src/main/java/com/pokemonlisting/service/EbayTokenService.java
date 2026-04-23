package com.pokemonlisting.service;

import com.pokemonlisting.config.EbayCredentialsConfig;
import org.springframework.stereotype.Service;

@Service
public class EbayTokenService {

    private final EbayCredentialsConfig ebayCredentialsConfig;

    public EbayTokenService(EbayCredentialsConfig ebayCredentialsConfig){
        this.ebayCredentialsConfig = ebayCredentialsConfig;
    }

    public String getUserAccessToken(){
        return ebayCredentialsConfig.getUserToken();
    }

    public String getBearerToken(){
        return "Bearer " + getUserAccessToken();
    }

    public String getBaseUrl(){
        if(ebayCredentialsConfig.isSandbox()) return "https://api.sandbox.ebay.com";
        else return "https://api.ebay.com";
    }
    
    public String getMarketplaceId(){
        return "EBAY_US";
    }

}
