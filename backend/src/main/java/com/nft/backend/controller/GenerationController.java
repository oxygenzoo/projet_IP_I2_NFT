package com.nft.backend.controller;

import java.util.List;

import com.nft.backend.dto.generation.GenerationResponse;
import com.nft.backend.service.AiGenerationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/generation")
public class GenerationController {

    private final AiGenerationService aiGenerationService;

    public GenerationController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @PostMapping(value = "/jobs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GenerationResponse createGenerationJob(
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(defaultValue = "Mon voyage") String title,
            @RequestParam(defaultValue = "") String destination,
            @RequestParam(defaultValue = "{}") String preferences) {
        return aiGenerationService.generateEpisode(images, title, destination, preferences);
    }
}
