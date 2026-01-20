package com.analysiscore.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ranking_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RankingItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ManyToOne
	@JoinColumn(name = "snapshot_id", nullable = false)
	private RankingSnapshot snapshot;

    @Column(name = "rank_value", nullable = false)
    private int rank;

    @Column(name = "brand_name", nullable = false, length = 200)
    private String brandName;

    @Column(name = "product_name", nullable = false, length = 300)
    private String productName;

    @Column(nullable = false)
    private long price;

    @Column(name = "product_url", length = 500)
    private String productUrl;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	private RankingItem(RankingSnapshot snapshot,
						int rank,
						String brandName,
						String productName,
						long price,
						String productUrl,
						String imageUrl) {
		this.snapshot = snapshot;
		this.rank = rank;
		this.brandName = brandName;
		this.productName = productName;
		this.price = price;
		this.productUrl = productUrl;
		this.imageUrl = imageUrl;
	}

	public static RankingItem ofParsed(int rank,
									   String brandName,
									   String productName,
									   long price,
									   String productUrl,
									   String imageUrl) {
		return new RankingItem(null, rank, brandName, productName, price, productUrl, imageUrl);
	}

	public static RankingItem of(RankingSnapshot snapshot,
								 int rank,
								 String brandName,
								 String productName,
								 long price,
								 String productUrl,
								 String imageUrl) {
		return new RankingItem(snapshot, rank, brandName, productName, price, productUrl, imageUrl);
	}
}
