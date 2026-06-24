package com.nft.backend.repository;

import java.util.List;
import java.util.UUID;

import com.nft.backend.model.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    List<Photo> findByTravelIdOrderByUploadedAtAsc(UUID travelId);
}
