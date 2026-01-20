package com.analysiscore.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class HourBucket {
    private static final DateTimeFormatter KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHxx");

    private final OffsetDateTime hourBucketAt;
    private final String hourBucketKey;

    private HourBucket(OffsetDateTime hourBucketAt, String hourBucketKey) {
        this.hourBucketAt = hourBucketAt;
        this.hourBucketKey = hourBucketKey;
    }

    public static HourBucket of(OffsetDateTime capturedAt, ZoneOffset offset, SourceType source) {
        OffsetDateTime bucketAt = capturedAt.withOffsetSameInstant(offset)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
        String key = source.name() + "|" + bucketAt.format(KEY_FORMATTER);
        return new HourBucket(bucketAt, key);
    }

    public OffsetDateTime getHourBucketAt() {
        return hourBucketAt;
    }

    public String getHourBucketKey() {
        return hourBucketKey;
    }
}
