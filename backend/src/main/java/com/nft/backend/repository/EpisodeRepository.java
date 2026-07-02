package com.nft.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeRepository extends JpaRepository<Episode, UUID> {

    List<Episode> findByTravelIdOrderByEpisodeNumberAsc(UUID travelId);

    Optional<Episode> findByShareToken(String shareToken);

    @Query("""
            select distinct e
            from Episode e
            left join EpisodeCollaborator c on c.episode = e
            where e.travel.userId = :userId
               or c.userId = :userId
               or (:email <> '' and lower(c.email) = lower(:email))
            order by e.generatedAt desc
            """)
    List<Episode> findAccessibleByIdentity(@Param("userId") UUID userId, @Param("email") String email);

    @Query("""
            select e
            from Episode e
            left join EpisodeCollaborator c on c.episode = e
            where e.id = :episodeId
              and (
                e.travel.userId = :userId
                or c.userId = :userId
                or (:email <> '' and lower(c.email) = lower(:email))
              )
            """)
    Optional<Episode> findAccessibleByIdentityAndId(
            @Param("episodeId") UUID episodeId,
            @Param("userId") UUID userId,
            @Param("email") String email);
}
