package com.nft.backend.repository;

import java.util.UUID;

import com.nft.backend.model.EpisodeCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeCollaboratorRepository extends JpaRepository<EpisodeCollaborator, UUID> {

    boolean existsByEpisodeIdAndUserId(UUID episodeId, UUID userId);

    boolean existsByEpisodeIdAndEmailIgnoreCase(UUID episodeId, String email);

    @Query("""
            select count(c) > 0
            from EpisodeCollaborator c
            where c.episode.travel.id = :travelId
              and (
                (:userId is not null and c.userId = :userId)
                or (:email <> '' and lower(c.email) = lower(:email))
              )
            """)
    boolean existsByTravelAndIdentity(@Param("travelId") UUID travelId, @Param("userId") UUID userId, @Param("email") String email);
}
