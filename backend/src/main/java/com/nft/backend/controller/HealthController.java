package com.nft.backend.controller;

import com.nft.backend.dto.HealthResponse;
import com.nft.backend.service.HealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthController.class);

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        LOGGER.debug("GET /health");
        return healthService.getHealth();
    }
}
