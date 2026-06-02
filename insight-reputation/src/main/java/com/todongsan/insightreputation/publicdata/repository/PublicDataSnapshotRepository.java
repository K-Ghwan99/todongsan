package com.todongsan.insightreputation.publicdata.repository;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDataSnapshotRepository extends JpaRepository<PublicDataSnapshot, Long> {
    
    Optional<PublicDataSnapshot> findBySourceAndDataTypeAndReferenceDateAndSourceRegionIdAndItmId(
            PublicDataSource source, PublicDataType dataType, LocalDate referenceDate, String sourceRegionId, String itmId);
    
    List<PublicDataSnapshot> findBySourceAndDataTypeAndReferenceDate(
            PublicDataSource source, PublicDataType dataType, LocalDate referenceDate);
    
    List<PublicDataSnapshot> findByRegionSidoAndDataTypeAndReferenceDateOrderByCollectedAtDesc(
            String regionSido, PublicDataType dataType, LocalDate referenceDate);
    
    @Query("SELECT pds FROM PublicDataSnapshot pds WHERE pds.regionFullpath LIKE :pathPrefix AND pds.dataType = :dataType AND pds.referenceDate = :referenceDate ORDER BY pds.collectedAt DESC")
    List<PublicDataSnapshot> findByRegionFullpathStartingWithAndDataTypeAndReferenceDate(
            @Param("pathPrefix") String pathPrefix, 
            @Param("dataType") PublicDataType dataType, 
            @Param("referenceDate") LocalDate referenceDate);
    
    @Query("SELECT pds FROM PublicDataSnapshot pds WHERE pds.source = :source AND pds.dataType = :dataType AND pds.sourceRegionId = :regionId AND pds.referenceDate BETWEEN :startDate AND :endDate ORDER BY pds.referenceDate DESC")
    List<PublicDataSnapshot> findTimeSeriesData(
            @Param("source") PublicDataSource source,
            @Param("dataType") PublicDataType dataType,
            @Param("regionId") String regionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 공공 데이터 배치 적재 (ON DUPLICATE KEY UPDATE)
     * 멱등성 보장을 위해 UNIQUE KEY 중복 시 UPDATE 처리
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO public_data_snapshot 
        (source, data_type, reference_date, region_sido, source_region_id, region_fullpath, 
         itm_id, itm_nm, numeric_value, raw_data, collected_at, created_at, updated_at)
        VALUES (:source, :dataType, :referenceDate, :regionSido, :sourceRegionId, :regionFullpath,
                :itmId, :itmNm, :numericValue, :rawData, :collectedAt, :createdAt, :updatedAt)
        ON DUPLICATE KEY UPDATE
            itm_nm = VALUES(itm_nm),
            numeric_value = VALUES(numeric_value),
            region_fullpath = VALUES(region_fullpath),
            raw_data = VALUES(raw_data),
            collected_at = VALUES(collected_at),
            updated_at = VALUES(updated_at)
        """, nativeQuery = true)
    void upsertSnapshot(
        @Param("source") String source,
        @Param("dataType") String dataType,
        @Param("referenceDate") LocalDate referenceDate,
        @Param("regionSido") String regionSido,
        @Param("sourceRegionId") String sourceRegionId,
        @Param("regionFullpath") String regionFullpath,
        @Param("itmId") String itmId,
        @Param("itmNm") String itmNm,
        @Param("numericValue") BigDecimal numericValue,
        @Param("rawData") String rawData,
        @Param("collectedAt") LocalDateTime collectedAt,
        @Param("createdAt") LocalDateTime createdAt,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}