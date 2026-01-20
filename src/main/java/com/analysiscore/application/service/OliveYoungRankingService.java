package com.analysiscore.application.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.analysiscore.application.parser.OliveYoungRankingParser;
import com.analysiscore.config.OyRankProperties;
import com.analysiscore.domain.entity.RankingItem;
import com.analysiscore.domain.entity.RankingSnapshot;
import com.analysiscore.domain.repository.RankingItemRepository;
import com.analysiscore.domain.repository.RankingSnapshotRepository;
import com.analysiscore.infrastructure.OliveYoungBrowserClient;
import com.analysiscore.model.HourBucket;
import com.analysiscore.model.SnapshotStatus;
import com.analysiscore.model.SourceConfig;
import com.analysiscore.model.SourceResponse;
import com.analysiscore.model.SourceType;

@Service
public class OliveYoungRankingService {
	private static final Logger log = LoggerFactory.getLogger(OliveYoungRankingService.class);

	private final OyRankProperties properties;
	private final OliveYoungBrowserClient client;
	private final OliveYoungRankingParser parser;
	private final RankingSnapshotRepository snapshotRepository;
	private final RankingItemRepository rankingItemRepository;

	public OliveYoungRankingService(OyRankProperties properties,
		OliveYoungBrowserClient client,
		OliveYoungRankingParser parser,
		RankingSnapshotRepository snapshotRepository,
		RankingItemRepository rankingItemRepository) {
		this.properties = properties;
		this.client = client;
		this.parser = parser;
		this.snapshotRepository = snapshotRepository;
		this.rankingItemRepository = rankingItemRepository;
	}

	@Transactional
	public RankingSnapshot collect(SourceType source) {
		SourceConfig config = resolveConfig(source);
		ZoneOffset offset = config.getOffset();
		OffsetDateTime capturedAt = OffsetDateTime.now(offset);
		HourBucket bucket = HourBucket.of(capturedAt, offset, source);
		long startedAt = System.nanoTime();

		try {
			SourceResponse response = client.fetchRendered(config);
			List<RankingItem> parsedItems = parser.parse(response.getContentType(), response.getBody());
			List<RankingItem> items = dedupeByRank(parsedItems);
			long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

			if (items.isEmpty()) {
				log.warn("No items parsed for {} bucket {} elapsedMs={}", source, bucket.getHourBucketKey(), elapsedMs);
				return saveSnapshot(
					source,
					capturedAt,
					bucket.getHourBucketAt(),
					bucket.getHourBucketKey(),
					response.getRawUrl(),
					SnapshotStatus.FAILED,
					"no items parsed",
					List.of()
				);
			}

			log.info(
				"Collected ranking items source={} bucket={} items={} elapsedMs={}",
				source,
				bucket.getHourBucketKey(),
				items.size(),
				elapsedMs
			);
			return saveSnapshot(
				source,
				capturedAt,
				bucket.getHourBucketAt(),
				bucket.getHourBucketKey(),
				response.getRawUrl(),
				SnapshotStatus.SUCCESS,
				null,
				items
			);
		} catch (Exception ex) {
			String message = ex.getMessage() == null ? "unexpected error" : ex.getMessage();
			long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
			if (ex instanceof OliveYoungBrowserClient.RenderedFetchException rendered) {
				log.warn(
					"Rendered fetch failed source={} bucket={} url={} status={} contentType={} title={} elapsedMs={} bodyPrefix={} errorType={}",
					source,
					bucket.getHourBucketKey(),
					rendered.getUrl(),
					rendered.getStatus(),
					rendered.getContentType(),
					rendered.getTitle(),
					elapsedMs,
					rendered.getBodyPrefix(),
					ex.getClass().getSimpleName(),
					ex
				);
			} else {
				log.warn(
					"Failed to collect ranking snapshot for {} bucket {} elapsedMs={}",
					source,
					bucket.getHourBucketKey(),
					elapsedMs,
					ex
				);
			}
			return saveSnapshot(
				source,
				capturedAt,
				bucket.getHourBucketAt(),
				bucket.getHourBucketKey(),
				config.resolveUrl(),
				SnapshotStatus.FAILED,
				message,
				List.of()
			);
		}
	}

	private SourceConfig resolveConfig(SourceType source) {
		if (source == SourceType.OLIVEYOUNG_KR) {
			OyRankProperties.SourceConfigProperties sourceProps = properties.getSource().getKr();
			ZoneOffset offset = ZoneOffset.of(sourceProps.getOffset());
			return new SourceConfig(
				SourceType.OLIVEYOUNG_KR,
				sourceProps.getBaseUrl(),
				sourceProps.getBestListPath(),
				offset
			);
		}
		throw new IllegalArgumentException("unsupported source: " + source);
	}

	private List<RankingItem> dedupeByRank(List<RankingItem> items) {
		Map<Integer, RankingItem> deduped = new LinkedHashMap<>();
		for (RankingItem item : items) {
			if (!deduped.containsKey(item.getRank())) {
				deduped.put(item.getRank(), item);
			}
		}
		return new ArrayList<>(deduped.values());
	}

	private RankingSnapshot saveSnapshot(SourceType source,
											   OffsetDateTime capturedAt,
											   OffsetDateTime hourBucketAt,
											   String hourBucketKey,
											   String rawUrl,
											   SnapshotStatus status,
											   String errorMessage,
											   List<RankingItem> items) {
		RankingSnapshot entity = RankingSnapshot.of(
			source,
			capturedAt,
			hourBucketAt,
			hourBucketKey,
			rawUrl,
			status,
			errorMessage,
			items.size()
		);
		RankingSnapshot saved = snapshotRepository.save(entity);

		if (items.isEmpty()) {
			return saved;
		}

		List<RankingItem> itemEntities = new ArrayList<>();
		for (RankingItem item : items) {
			RankingItem itemEntity = RankingItem.of(
				saved,
				item.getRank(),
				item.getBrandName(),
				item.getProductName(),
				item.getPrice(),
				item.getProductUrl(),
				item.getImageUrl()
			);
			itemEntities.add(itemEntity);
		}
		rankingItemRepository.saveAll(itemEntities);
		return saved;
	}
}


