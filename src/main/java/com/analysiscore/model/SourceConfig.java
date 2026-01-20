package com.analysiscore.model;

import java.time.ZoneOffset;
import java.util.Objects;

public final class SourceConfig {
    private final SourceType source;
    private final String baseUrl;
    private final String bestListPath;
    private final ZoneOffset offset;

    public SourceConfig(SourceType source, String baseUrl, String bestListPath, ZoneOffset offset) {
        this.source = Objects.requireNonNull(source, "source");
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.bestListPath = Objects.requireNonNull(bestListPath, "bestListPath");
        this.offset = Objects.requireNonNull(offset, "offset");
    }

    public SourceType getSource() {
        return source;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getBestListPath() {
        return bestListPath;
    }

    public ZoneOffset getOffset() {
        return offset;
    }

    public String resolveUrl() {
        if (bestListPath.startsWith("http")) {
            return bestListPath;
        }
        if (baseUrl.endsWith("/") && bestListPath.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + bestListPath;
        }
        return baseUrl + bestListPath;
    }
}
