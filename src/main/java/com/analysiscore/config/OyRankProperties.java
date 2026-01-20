package com.analysiscore.config;

import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oyrank")
public class OyRankProperties {
    private final SourceProperties source = new SourceProperties();
    private final HttpProperties http = new HttpProperties();

    public SourceProperties getSource() {
        return source;
    }

    public HttpProperties getHttp() {
        return http;
    }

    public static class SourceProperties {
        private SourceConfigProperties kr = new SourceConfigProperties();

        public SourceConfigProperties getKr() {
            return kr;
        }

        public void setKr(SourceConfigProperties kr) {
            this.kr = Objects.requireNonNull(kr, "kr");
        }
    }

    public static class SourceConfigProperties {
        private String code;
        private String baseUrl;
        private String bestListPath;
        private String offset;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getBestListPath() {
            return bestListPath;
        }

        public void setBestListPath(String bestListPath) {
            this.bestListPath = bestListPath;
        }

        public String getOffset() {
            return offset;
        }

        public void setOffset(String offset) {
            this.offset = offset;
        }
    }

    public static class HttpProperties {
        private long timeoutMillis = 5000;
        private RetryProperties retry = new RetryProperties();

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public RetryProperties getRetry() {
            return retry;
        }

        public void setRetry(RetryProperties retry) {
            this.retry = retry;
        }
    }

    public static class RetryProperties {
        private int maxAttempts = 2;
        private long backoffMillis = 500;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMillis() {
            return backoffMillis;
        }

        public void setBackoffMillis(long backoffMillis) {
            this.backoffMillis = backoffMillis;
        }
    }
}
