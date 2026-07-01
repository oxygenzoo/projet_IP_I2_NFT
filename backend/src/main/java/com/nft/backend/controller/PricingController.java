package com.nft.backend.controller;

import java.util.List;

import com.nft.backend.dto.pricing.PricingPlanDto;
import com.nft.backend.service.PricingPlanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing-plans")
public class PricingController {

    private final PricingPlanService pricingPlanService;

    public PricingController(PricingPlanService pricingPlanService) {
        this.pricingPlanService = pricingPlanService;
    }

    @GetMapping
    public List<PricingPlanDto> getPricingPlans() {
        return pricingPlanService.getPlans();
    }
}
