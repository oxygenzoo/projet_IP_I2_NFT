package com.nft.backend.dto.ai;

import java.util.List;

public record PhotoAnalysisDto(
        String photoId,
        int qualityScore,
        List<String> tags,
        String location) {
}
