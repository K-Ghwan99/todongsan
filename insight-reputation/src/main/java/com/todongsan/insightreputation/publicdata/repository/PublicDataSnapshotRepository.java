package com.todongsan.insightreputation.publicdata.repository;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import com.todongsan.insightreputation.publicdata.entity.PublicDataSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDataSnapshotRepository extends JpaRepository<PublicDataSnapshot, Long> {
    
    Optional<PublicDataSnapshot> findBySourceAndDataTypeAndReferenceDateAndSourceRegionId(
            PublicDataSource source, PublicDataType dataType, LocalDate referenceDate, String sourceRegionId);
    
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
}