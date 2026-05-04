package com.pokemonlisting.repository;

import com.pokemonlisting.model.UploadedImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadedImageRepository extends JpaRepository<UploadedImage, Long> {
    Optional<UploadedImage> findBySavedFilename(String savedFilename);
}
