package com.nft.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.Preference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository extends JpaRepository<Preference, UUID> {

    Optional<Preference> findByTravelId(UUID travelId);
}
