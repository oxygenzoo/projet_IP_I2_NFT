package com.nft.backend.dto.travel;

public record SceneDto(
        String id,
        int order,
        String title,
        String timecode,
        String voiceOverText,
        String type,
        String generationStatus,
        String imageUrl,
        Boolean isAiReconstructed,
        String aiPrompt) {

    public SceneDto(
            String id,
            String title,
            String timecode,
            String voiceOverText,
            String imageUrl,
            Boolean isAiReconstructed,
            String aiPrompt) {
        this(
                id,
                0,
                title,
                timecode,
                voiceOverText,
                "souvenir",
                isAiReconstructed ? "ai_reconstructed" : "generated",
                imageUrl,
                isAiReconstructed,
                aiPrompt);
    }
}
