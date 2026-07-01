package com.nft.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.travel.CreateTravelRequest;
import com.nft.backend.dto.travel.TravelResponse;
import com.nft.backend.dto.travel.UpdateTravelRequest;
import com.nft.backend.model.Profile;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.ProfileRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TravelService {

    private final TravelRepository travelRepository;
    private final ProfileRepository profileRepository;

    public TravelService(TravelRepository travelRepository, ProfileRepository profileRepository) {
        this.travelRepository = travelRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public TravelResponse create(CreateTravelRequest request) {
        validateDates(request.startDate(), request.endDate());
        Profile user = findUser(request.userId());
        Travel travel = new Travel(
                user,
                request.title().trim(),
                clean(request.destination()),
                clean(request.description()),
                request.startDate(),
                request.endDate());

        return TravelResponse.fromEntity(travelRepository.save(travel));
    }

    @Transactional(readOnly = true)
    public List<TravelResponse> getAll() {
        return travelRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(TravelResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TravelResponse> getByUser(UUID userId) {
        if (!profileRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found");
        }

        return travelRepository.findByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(TravelResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TravelResponse get(UUID travelId) {
        return TravelResponse.fromEntity(findTravel(travelId));
    }

    @Transactional
    public TravelResponse update(UUID travelId, UpdateTravelRequest request) {
        validateDates(request.startDate(), request.endDate());
        Travel travel = findTravel(travelId);
        travel.update(
                request.title().trim(),
                clean(request.destination()),
                clean(request.description()),
                request.startDate(),
                request.endDate());

        return TravelResponse.fromEntity(travelRepository.save(travel));
    }

    @Transactional
    public void delete(UUID travelId) {
        travelRepository.delete(findTravel(travelId));
    }

    private Travel findTravel(UUID travelId) {
        return travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
    }

    private Profile findUser(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be before end date");
        }
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
