package com.nft.backend.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.CreationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreationSessionRepository extends JpaRepository<CreationSession, UUID> {

    Optional<CreationSession> findTopByOwnerIdAndStatusInOrderByUpdatedAtDesc(UUID ownerId, Collection<String> statuses);
}
