package com.pokemonlisting.repository;

import com.pokemonlisting.model.EbayToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EbayTokenRepository extends JpaRepository<EbayToken, Long> {

    Optional<EbayToken> findByUserId(String userId);
}
