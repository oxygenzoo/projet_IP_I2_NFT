package com.nft.backend.dto.pricing;

import java.util.List;

public record PricingPlanDto(
        String id,
        String name,
        String price,
        String cadence,
        String description,
        List<String> features,
        boolean highlighted,
        String audience) {
}
