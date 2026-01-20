package com.analysiscore.model;

import java.util.Objects;

public final class SourceResponse {
    private final String contentType;
    private final String body;
    private final String rawUrl;

    public SourceResponse(String contentType, String body, String rawUrl) {
        this.contentType = contentType;
        this.body = Objects.requireNonNull(body, "body");
        this.rawUrl = Objects.requireNonNull(rawUrl, "rawUrl");
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }

    public String getRawUrl() {
        return rawUrl;
    }
}
