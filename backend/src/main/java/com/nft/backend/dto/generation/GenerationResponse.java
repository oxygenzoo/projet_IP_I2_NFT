package com.nft.backend.dto.generation;

import java.util.List;
import java.util.Map;

public record GenerationResponse(
        String job_id,
        String status,
        String message,
        Map<String, Object> selection_report,
        Map<String, Object> script,
        List<String> videos,
        String workdir) {
}
