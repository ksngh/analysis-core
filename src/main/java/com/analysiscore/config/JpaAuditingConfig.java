package com.analysiscore.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
@Configuration
public class JpaAuditingConfig {

	@Bean("offsetDateTimeProvider")
	public DateTimeProvider offsetDateTimeProvider() {
		final Clock systemUtcClock = Clock.systemUTC();
		return () -> Optional.of(OffsetDateTime.now(systemUtcClock));
	}

}
