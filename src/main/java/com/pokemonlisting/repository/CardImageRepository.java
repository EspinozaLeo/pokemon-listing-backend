package com.pokemonlisting.repository;

import com.pokemonlisting.model.CardImage;
import com.pokemonlisting.model.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardImageRepository extends JpaRepository<CardImage, Long>{
    List<CardImage> findByCardIdOrderByDisplayOrderAsc(Long cardId);
    Optional<CardImage> findByCardIdAndImageType(Long cardId, ImageType imageType);
    Optional<CardImage> findByUploadedImageId(Long uploadedImageId);
    long countByCardId(Long cardId);
}