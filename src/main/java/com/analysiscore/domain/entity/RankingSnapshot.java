package com.analysiscore.domain.entity;

import com.analysiscore.model.SnapshotStatus;
import com.analysiscore.model.SourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ranking_snapshot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingSnapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private SourceType source;

	@Column(name = "captured_at", nullable = false)
	private OffsetDateTime capturedAt;

	@Column(name = "hour_bucket_at", nullable = false)
	private OffsetDateTime hourBucketAt;

	@Column(name = "hour_bucket_key", nullable = false, length = 100)
	private String hourBucketKey;

	@Column(name = "raw_url", nullable = false, length = 500)
	private String rawUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SnapshotStatus status;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(name = "item_count", nullable = false)
	private int itemCount;

	private RankingSnapshot(SourceType source,
								  OffsetDateTime capturedAt,
								  OffsetDateTime hourBucketAt,
								  String hourBucketKey,
								  String rawUrl,
								  SnapshotStatus status,
								  String errorMessage,
								  int itemCount) {
		this.source = source;
		this.capturedAt = capturedAt;
		this.hourBucketAt = hourBucketAt;
		this.hourBucketKey = hourBucketKey;
		this.rawUrl = rawUrl;
		this.status = status;
		this.errorMessage = errorMessage;
		this.itemCount = itemCount;
	}

	public static RankingSnapshot of(SourceType source,
										   OffsetDateTime capturedAt,
										   OffsetDateTime hourBucketAt,
										   String hourBucketKey,
										   String rawUrl,
										   SnapshotStatus status,
										   String errorMessage,
										   int itemCount) {
		return new RankingSnapshot(
			source,
			capturedAt,
			hourBucketAt,
			hourBucketKey,
			rawUrl,
			status,
			errorMessage,
			itemCount
		);
	}
}
