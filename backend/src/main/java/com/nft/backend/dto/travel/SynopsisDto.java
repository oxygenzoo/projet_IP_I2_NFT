package com.nft.backend.dto.travel;

import java.util.List;

public record SynopsisDto(
        String title,
        String summary,
        List<String> keyMoments) {
}
