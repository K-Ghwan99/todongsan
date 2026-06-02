package com.todongsan.insightreputation.publicdata.dto;

import com.todongsan.insightreputation.enums.PublicDataSource;
import com.todongsan.insightreputation.enums.PublicDataType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ParsedDataRow {
    
    private PublicDataSource source;
    private PublicDataType dataType;
    private LocalDate referenceDate;
    private String regionSido;
    private String sourceRegionId;
    private String regionFullpath;
    private BigDecimal numericValue;
    private String rawData;
}