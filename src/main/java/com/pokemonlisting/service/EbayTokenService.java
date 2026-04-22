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
}
