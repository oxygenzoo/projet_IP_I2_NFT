package com.nft.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.nft.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {

    Optional<Profile> findByEmail(String email);

    boolean existsByEmail(String email);
}
