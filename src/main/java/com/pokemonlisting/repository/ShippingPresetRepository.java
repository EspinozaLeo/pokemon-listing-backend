package com.pokemonlisting.repository;

import com.pokemonlisting.model.ShippingPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShippingPresetRepository extends JpaRepository<ShippingPreset, Long> {

}
