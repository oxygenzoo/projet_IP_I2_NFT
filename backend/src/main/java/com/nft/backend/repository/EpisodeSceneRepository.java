package com.nft.backend.repository;

import java.util.List;
import java.util.UUID;

import com.nft.backend.model.EpisodeScene;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeSceneRepository extends JpaRepository<EpisodeScene, UUID> {

    List<EpisodeScene> findByEpisodeIdOrderByOrderAsc(UUID episodeId);
}
