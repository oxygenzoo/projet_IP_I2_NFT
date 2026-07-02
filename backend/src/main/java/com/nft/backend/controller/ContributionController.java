package com.nft.backend.controller;

import java.net.URI;
import java.util.UUID;

import com.nft.backend.dto.contribution.ContributionLinkResponse;
import com.nft.backend.dto.contribution.PublicContributionResponse;
import com.nft.backend.dto.photo.PhotoResponse;
import com.nft.backend.service.ContributionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @PostMapping("/travels/{travelId}/contribution-links")
    public ResponseEntity<ContributionLinkResponse> createLink(@PathVariable UUID travelId) {
        ContributionLinkResponse link = contributionService.create(travelId);
        return ResponseEntity.created(URI.create("/api/contributions/" + link.token())).body(link);
    }

    @GetMapping("/contributions/{token}")
    public PublicContributionResponse getContributionInfo(@PathVariable String token) {
        return contributionService.getPublicInfo(token);
    }

    @PostMapping(value = "/contributions/{token}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadContributionPhoto(
            @PathVariable String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean consentRgpd) {
        PhotoResponse photo = contributionService.upload(token, file, consentRgpd);
        return ResponseEntity.created(URI.create(photo.imageUrl())).body(photo);
    }
}
