package com.pokemonlisting.repository;

import com.pokemonlisting.model.Card;
import com.pokemonlisting.model.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CardRepository extends  JpaRepository<Card, Long>{
    List<Card> findByStatus(CardStatus status);
    List<Card> findAllByOrderByCreatedAtDesc();
    long countByStatus(CardStatus status);
}