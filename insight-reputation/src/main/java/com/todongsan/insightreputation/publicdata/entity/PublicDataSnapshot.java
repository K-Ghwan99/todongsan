package com.todongsan.insightreputation.publicdata.entity;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.global.config.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "public_data_snapshot",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_snapshot", 
        columnNames = {"source", "data_type", "reference_date", "source_region_id"}
    ),
    indexes = {
        @Index(name = "idx_source_type_date", columnList = "source, data_type, reference_date"),
        @Index(name = "idx_region_sido", columnList = "region_sido"),
        @Index(name = "idx_region_fullpath", columnList = "region_fullpath(100)")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicDataSnapshot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private PublicDataSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private PublicDataType dataType;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(name = "region_sido", length = 50)
    private String regionSido;

    @Column(name = "source_region_id", nullable = false, length = 50)
    private String sourceRegionId;

    @Column(name = "region_fullpath", length = 200)
    private String regionFullpath;

    @Column(name = "numeric_value", precision = 20, scale = 10)
    private BigDecimal numericValue;

    @Column(name = "raw_data", nullable = false, columnDefinition = "JSON")
    private String rawData;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @Builder
    public PublicDataSnapshot(PublicDataSource source, PublicDataType dataType, LocalDate referenceDate,
                             String regionSido, String sourceRegionId, String regionFullpath,
                             BigDecimal numericValue, String rawData) {
        this.source = source;
        this.dataType = dataType;
        this.referenceDate = referenceDate;
        this.regionSido = regionSido;
        this.sourceRegionId = sourceRegionId;
        this.regionFullpath = regionFullpath;
        this.numericValue = numericValue;
        this.rawData = rawData;
        this.collectedAt = LocalDateTime.now();
    }

    public void updateSnapshot(String regionSido, String regionFullpath, BigDecimal numericValue, String rawData) {
        this.regionSido = regionSido;
        this.regionFullpath = regionFullpath;
        this.numericValue = numericValue;
        this.rawData = rawData;
        this.collectedAt = LocalDateTime.now();
    }
}