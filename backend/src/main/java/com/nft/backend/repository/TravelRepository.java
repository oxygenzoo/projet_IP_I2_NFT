package com.nft.backend.repository;

import java.util.List;
import java.util.UUID;

import com.nft.backend.model.Travel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRepository extends JpaRepository<Travel, UUID> {

    List<Travel> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
