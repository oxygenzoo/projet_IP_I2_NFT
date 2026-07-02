package com.nft.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.ContributionLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionLinkRepository extends JpaRepository<ContributionLink, UUID> {

    Optional<ContributionLink> findByToken(String token);
}
