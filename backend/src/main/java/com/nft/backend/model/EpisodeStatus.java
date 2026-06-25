package com.nft.backend.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EpisodeStatus {
    DRAFT("draft"),
    GENERATING("generating"),
    READY("ready"),
    FAILED("failed");

    private final String value;

    EpisodeStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static EpisodeStatus fromValue(String value) {
        return Arrays.stream(values())
                .filter((status) -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown episode status: " + value));
    }
}
