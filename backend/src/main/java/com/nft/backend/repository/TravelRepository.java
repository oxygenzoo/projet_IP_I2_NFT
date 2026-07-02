package com.nft.backend.repository;

import java.util.List;
import java.util.UUID;

import com.nft.backend.model.Travel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelRepository extends JpaRepository<Travel, UUID> {

    List<Travel> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            select distinct t
            from Travel t
            left join t.episodes e
            left join EpisodeCollaborator c on c.episode = e
            where t.userId = :userId
               or c.userId = :userId
               or (:email <> '' and lower(c.email) = lower(:email))
            order by t.createdAt desc
            """)
    List<Travel> findAccessibleByIdentity(@Param("userId") UUID userId, @Param("email") String email);
}
