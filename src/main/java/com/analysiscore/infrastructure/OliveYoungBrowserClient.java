package com.analysiscore.infrastructure;

import com.analysiscore.config.OyRankProperties;
import com.analysiscore.model.SourceConfig;
import com.analysiscore.model.SourceResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitUntilState;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OliveYoungBrowserClient {
    private static final Logger log = LoggerFactory.getLogger(OliveYoungBrowserClient.class);
    private static final int BODY_PREFIX_LIMIT = 2048;
    private static final String[] BLOCK_PHRASES = {
        "access denied",
        "forbidden",
        "차단",
        "접근",
        "권한이 없습니다"
    };

    private final OyRankProperties properties;

    public OliveYoungBrowserClient(OyRankProperties properties) {
        this.properties = properties;
    }

    public SourceResponse fetchRendered(SourceConfig sourceConfig) {
        String url = sourceConfig.resolveUrl();
        long timeoutMillis = properties.getHttp().getTimeoutMillis();
        int maxAttempts = Math.max(1, properties.getHttp().getRetry().getMaxAttempts());
        long backoffMillis = properties.getHttp().getRetry().getBackoffMillis();

        RenderedFetchException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return fetchOnce(url, timeoutMillis);
            } catch (AccessDeniedException ex) {
                throw ex;
            } catch (RenderedFetchException ex) {
                last = ex;
                if (!isRetryable(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                sleep(backoffMillis);
            }
        }
        throw last == null
            ? new RenderedFetchException("rendered fetch failed", url, null, null, null, null)
            : last;
    }

    private SourceResponse fetchOnce(String url, long timeoutMillis) {
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(true);
            try (Browser browser = playwright.chromium().launch(launchOptions)) {
                try (BrowserContext context = browser.newContext()) {
                    try (Page page = context.newPage()) {
                        Page.NavigateOptions navigateOptions = new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.NETWORKIDLE)
                            .setTimeout(timeoutMillis);
                        Response response = page.navigate(url, navigateOptions);

                        Integer status = response == null ? null : response.status();
                        String contentType = response == null ? null : response.headers().get("content-type");

                        if (status != null && status >= 400) {
                            String title = safeTitle(page);
                            String html = safeContent(page);
                            if (status == 401 || status == 403) {
                                throw new AccessDeniedException(
                                    "access denied",
                                    url,
                                    status,
                                    contentType,
                                    title,
                                    trimPrefix(html)
                                );
                            }
                            throw new RenderedFetchException(
                                "http error",
                                url,
                                status,
                                contentType,
                                title,
                                trimPrefix(html)
                            );
                        }

                        try {
                            page.waitForSelector(
                                "li:has(.tx_name), li:has(.tx_brand), [data-prd-name], [data-goods-name], [data-brand-name]",
                                new Page.WaitForSelectorOptions().setTimeout(timeoutMillis)
                            );
                        } catch (TimeoutError ex) {
                            String title = safeTitle(page);
                            String html = safeContent(page);
                            throw new RenderedFetchException(
                                "render timeout",
                                url,
                                status,
                                contentType,
                                title,
                                trimPrefix(html),
                                ex
                            );
                        }

                        String html = safeContent(page);
                        String title = safeTitle(page);
                        if (isBlocked(html, title)) {
                            throw new AccessDeniedException(
                                "blocked content detected",
                                url,
                                status,
                                contentType,
                                title,
                                trimPrefix(html)
                            );
                        }

                        return new SourceResponse(contentType, html, url);
                    }
                }
            }
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (PlaywrightException ex) {
            throw new RenderedFetchException(
                "render error",
                url,
                null,
                null,
                null,
                null,
                ex
            );
        }
    }

    private boolean isRetryable(RenderedFetchException ex) {
        if (ex instanceof AccessDeniedException) {
            return false;
        }
        return ex.getCause() instanceof TimeoutError;
    }

    private boolean isBlocked(String html, String title) {
        String content = (html == null ? "" : html) + " " + (title == null ? "" : title);
        String lowered = content.toLowerCase(Locale.ROOT);
        for (String phrase : BLOCK_PHRASES) {
            if (lowered.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private String safeTitle(Page page) {
        try {
            return page.title();
        } catch (PlaywrightException ex) {
            log.debug("Failed to read page title", ex);
            return null;
        }
    }

    private String safeContent(Page page) {
        try {
            return page.content();
        } catch (PlaywrightException ex) {
            log.debug("Failed to read page content", ex);
            return "";
        }
    }

    private String trimPrefix(String html) {
        if (html == null) {
            return "";
        }
        if (html.length() <= BODY_PREFIX_LIMIT) {
            return html;
        }
        return html.substring(0, BODY_PREFIX_LIMIT);
    }

    private void sleep(long backoffMillis) {
        if (backoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMillis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public static class RenderedFetchException extends RuntimeException {
        private final String url;
        private final Integer status;
        private final String contentType;
        private final String title;
        private final String bodyPrefix;

        public RenderedFetchException(String message,
                                      String url,
                                      Integer status,
                                      String contentType,
                                      String title,
                                      String bodyPrefix) {
            super(message);
            this.url = url;
            this.status = status;
            this.contentType = contentType;
            this.title = title;
            this.bodyPrefix = bodyPrefix;
        }

        public RenderedFetchException(String message,
                                      String url,
                                      Integer status,
                                      String contentType,
                                      String title,
                                      String bodyPrefix,
                                      Throwable cause) {
            super(message, cause);
            this.url = url;
            this.status = status;
            this.contentType = contentType;
            this.title = title;
            this.bodyPrefix = bodyPrefix;
        }

        public String getUrl() {
            return url;
        }

        public Integer getStatus() {
            return status;
        }

        public String getContentType() {
            return contentType;
        }

        public String getTitle() {
            return title;
        }

        public String getBodyPrefix() {
            return bodyPrefix;
        }
    }

    public static class AccessDeniedException extends RenderedFetchException {
        public AccessDeniedException(String message,
                                     String url,
                                     Integer status,
                                     String contentType,
                                     String title,
                                     String bodyPrefix) {
            super(message, url, status, contentType, title, bodyPrefix);
        }
    }
}
