package com.nft.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findByTravelIdOrderByEpisodeNumberAsc(UUID travelId);

    Optional<Episode> findByShareToken(String shareToken);
}
