package com.analysiscore.infrastructure;

import com.analysiscore.config.OyRankProperties;
import com.analysiscore.model.SourceConfig;
import com.analysiscore.model.SourceResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
public class OliveYoungRankingClient {
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; OYRankBot/1.0)";

    private final WebClient webClient;
    private final OyRankProperties properties;

    public OliveYoungRankingClient(OyRankProperties properties) {
        this.webClient = WebClient.builder().build();
        this.properties = properties;
    }

    public SourceResponse fetch(SourceConfig sourceConfig) {
        String url = sourceConfig.resolveUrl();
        long timeoutMillis = properties.getHttp().getTimeoutMillis();
        int maxAttempts = properties.getHttp().getRetry().getMaxAttempts();
        long backoffMillis = properties.getHttp().getRetry().getBackoffMillis();

        Mono<SourceResponse> responseMono = webClient.get()
            .uri(url)
            .header(HttpHeaders.USER_AGENT, USER_AGENT)
            .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
            .header(HttpHeaders.REFERER, "https://www.oliveyoung.co.kr/")
            .header(HttpHeaders.ORIGIN, "https://www.oliveyoung.co.kr")
            .exchangeToMono(response -> {
                String contentType = response.headers().contentType()
                    .map(MediaType::toString)
                    .orElse(null);
                return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> {
                        if (!response.statusCode().is2xxSuccessful()) {
                            return Mono.error(new IllegalStateException("HTTP " + response.statusCode().value()));
                        }
                        return Mono.just(new SourceResponse(contentType, body, url));
                    });
            })
            .timeout(Duration.ofMillis(timeoutMillis))
            .retryWhen(Retry.backoff(Math.max(0, maxAttempts - 1), Duration.ofMillis(backoffMillis))
                .filter(ex -> !(ex instanceof IllegalStateException)));

        return responseMono.block();
    }
}
