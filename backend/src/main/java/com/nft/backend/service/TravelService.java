package com.nft.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.nft.backend.dto.travel.SaveTravelRequest;
import com.nft.backend.dto.travel.TravelDto;
import com.nft.backend.model.Travel;
import com.nft.backend.model.User;
import com.nft.backend.repository.EpisodeCollaboratorRepository;
import com.nft.backend.repository.TravelRepository;
import com.nft.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TravelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TravelService.class);
    private static final String GENERATED_IMAGE = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 800'%3E%3Crect width='1200' height='800' fill='%23242b68'/%3E%3Cpath d='M0 610 210 470 390 560 620 370 820 515 1200 295v505H0z' fill='%23842f68'/%3E%3C/svg%3E";

    private final TravelRepository travelRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final EpisodeCollaboratorRepository collaboratorRepository;

    public TravelService(
            TravelRepository travelRepository,
            UserRepository userRepository,
            AuthenticatedUserService authenticatedUserService,
            EpisodeCollaboratorRepository collaboratorRepository) {
        this.travelRepository = travelRepository;
        this.userRepository = userRepository;
        this.authenticatedUserService = authenticatedUserService;
        this.collaboratorRepository = collaboratorRepository;
    }

    @Transactional
    public TravelDto create(SaveTravelRequest request) {
        User user = resolveOwner(request);
        LOGGER.info("Creating travel draft for userId={} title='{}'", user.getId(), request.title().trim());
        Travel travel = new Travel(
                user,
                request.title().trim(),
                clean(request.destination()),
                request.startDate(),
                request.endDate(),
                clean(request.description()));

        return toDto(travelRepository.save(travel));
    }

    @Transactional(readOnly = true)
    public List<TravelDto> getTravels() {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.requireCurrentUser();
        return travelRepository.findAccessibleByIdentity(user.id(), user.email())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TravelDto getTravel(UUID id) {
        Travel travel = findTravel(id);
        assertCanRead(travel);
        return toDto(travel);
    }

    @Transactional
    public TravelDto update(UUID id, SaveTravelRequest request) {
        Travel travel = findTravel(id);
        assertOwner(travel);
        User user = resolveOwner(request);
        travel.update(
                user,
                request.title().trim(),
                clean(request.destination()),
                request.startDate(),
                request.endDate(),
                clean(request.description()));

        return toDto(travelRepository.save(travel));
    }

    @Transactional
    public void delete(UUID id) {
        Travel travel = findTravel(id);
        assertOwner(travel);
        travelRepository.delete(travel);
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return travelRepository.existsById(id);
    }

    private Travel findTravel(UUID id) {
        return travelRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));
    }

    private User findUser(UUID id) {
        if (id == null) {
            return null;
        }

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User resolveOwner(SaveTravelRequest request) {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.requireCurrentUser();
        return userRepository.findById(user.id())
                .orElseGet(() -> userRepository.save(new User(
                        user.id(),
                        user.email().isBlank() ? user.id() + "@external.local" : user.email(),
                        "external",
                        true)));
    }

    private void assertOwner(Travel travel) {
        UUID currentUserId = authenticatedUserService.requireCurrentUserId();
        if (travel.getUser() == null || !currentUserId.equals(travel.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        }
    }

    private void assertCanRead(Travel travel) {
        AuthenticatedUserService.AuthenticatedUser user = authenticatedUserService.currentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        boolean owner = travel.getUser() != null && user.id().equals(travel.getUser().getId());
        if (!owner && !collaboratorRepository.existsByTravelAndIdentity(travel.getId(), user.id(), user.email())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        }
    }

    private TravelDto toDto(Travel travel) {
        String destination = clean(travel.getDestination());
        int year = yearFrom(travel.getStartDate());

        return new TravelDto(
                travel.getId().toString(),
                travel.getTitle(),
                destination,
                destination,
                year,
                destination.isBlank() ? "Voyage personnel" : "Souvenirs de " + destination,
                clean(travel.getDescription()),
                GENERATED_IMAGE,
                GENERATED_IMAGE,
                GENERATED_IMAGE,
                "0 min",
                travel.getEpisodes().size(),
                travel.getPhotos().size(),
                0,
                "À générer",
                false,
                List.of("Voyage"),
                List.of(),
                travel.getUser() == null ? null : travel.getUser().getId().toString(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getCreatedAt());
    }

    private int yearFrom(LocalDate startDate) {
        return startDate == null ? LocalDate.now().getYear() : startDate.getYear();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
