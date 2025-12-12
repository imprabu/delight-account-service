package com.delight.account.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Converts between OffsetDateTime (with nanosecond precision) and String for database storage.
 * Uses ISO-8601 format with nanosecond precision: yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnXXX
 * 
 * This allows storing nanosecond-precision timestamps as strings in the database
 * for versioning purposes while using OffsetDateTime in the Java model.
 */
@Converter(autoApply = false)
public class OffsetDateTimeStringConverter implements AttributeConverter<OffsetDateTime, String> {

    /**
     * Formatter for writing: always includes nanoseconds (0-9 digits)
     */
    private static final DateTimeFormatter WRITE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendOffsetId()
            .toFormatter();

    /**
     * Formatter for reading: flexible parsing of ISO-8601 with or without nanoseconds
     */
    private static final DateTimeFormatter READ_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .appendOffsetId()
            .toFormatter();

    @Override
    public String convertToDatabaseColumn(OffsetDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        // Always format with nanosecond precision (pads with zeros if needed)
        return attribute.format(WRITE_FORMATTER);
    }

    @Override
    public OffsetDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            // Try flexible parsing first
            return OffsetDateTime.parse(dbData.trim(), READ_FORMATTER);
        } catch (Exception e) {
            // Fallback to standard ISO-8601 parsing
            return OffsetDateTime.parse(dbData.trim());
        }
    }
}







