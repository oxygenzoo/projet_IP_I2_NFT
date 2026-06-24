package com.nft.backend.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EpisodeStatusConverter implements AttributeConverter<EpisodeStatus, String> {

    @Override
    public String convertToDatabaseColumn(EpisodeStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public EpisodeStatus convertToEntityAttribute(String value) {
        return value == null ? null : EpisodeStatus.fromValue(value);
    }
}
