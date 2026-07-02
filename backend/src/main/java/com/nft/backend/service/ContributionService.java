package com.nft.backend.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

import com.nft.backend.dto.contribution.ContributionLinkResponse;
import com.nft.backend.dto.contribution.PublicContributionResponse;
import com.nft.backend.dto.photo.PhotoResponse;
import com.nft.backend.model.ContributionLink;
import com.nft.backend.model.Travel;
import com.nft.backend.repository.ContributionLinkRepository;
import com.nft.backend.repository.TravelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContributionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ContributionLinkRepository contributionLinkRepository;
    private final TravelRepository travelRepository;
    private final PhotoService photoService;
    private final AuthenticatedUserService authenticatedUserService;
    private final String publicBaseUrl;

    public ContributionService(
            ContributionLinkRepository contributionLinkRepository,
            TravelRepository travelRepository,
            PhotoService photoService,
            AuthenticatedUserService authenticatedUserService,
            @Value("${app.public-url:http://localhost:4200}") String publicBaseUrl) {
        this.contributionLinkRepository = contributionLinkRepository;
        this.travelRepository = travelRepository;
        this.photoService = photoService;
        this.authenticatedUserService = authenticatedUserService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional
    public ContributionLinkResponse create(UUID travelId) {
        UUID ownerId = authenticatedUserService.requireCurrentUserId();
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel not found"));

        if (travel.getUser() == null || !ownerId.equals(travel.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Travel access denied");
        }

        ContributionLink link = new ContributionLink(
                travel,
                newToken(),
                ownerId,
                Instant.now().plus(14, ChronoUnit.DAYS));

        return ContributionLinkResponse.fromEntity(contributionLinkRepository.save(link), publicBaseUrl);
    }

    @Transactional(readOnly = true)
    public PublicContributionResponse getPublicInfo(String token) {
        ContributionLink link = findActiveLink(token);
        return new PublicContributionResponse(link.getTravel().getId(), link.getTravel().getTitle(), link.getExpiresAt());
    }

    @Transactional
    public PhotoResponse upload(String token, MultipartFile file, boolean consentRgpd) {
        ContributionLink link = findActiveLink(token);
        return photoService.uploadContribution(link.getTravel().getId(), file, consentRgpd);
    }

    private ContributionLink findActiveLink(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution link not found");
        }

        ContributionLink link = contributionLinkRepository.findByToken(token.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contribution link not found"));

        if (link.isExpired()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Contribution link expired");
        }

        return link;
    }

    private String newToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
