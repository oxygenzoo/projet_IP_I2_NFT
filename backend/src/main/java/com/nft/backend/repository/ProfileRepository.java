package com.nft.backend.repository;

import java.util.UUID;

import com.nft.backend.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
