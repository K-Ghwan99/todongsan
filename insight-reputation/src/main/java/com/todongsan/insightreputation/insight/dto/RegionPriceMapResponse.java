package com.todongsan.insightreputation.insight.dto;

import java.time.LocalDate;
import java.util.List;

public record RegionPriceMapResponse(
        LocalDate asOf,
        String dataType,
        List<RegionEntry> regions
) {
    public record RegionEntry(
            String regionSido,
            Double latestIndex,
            Double prevIndex,
            Double changePct,
            String direction,
            long visitCertCount
    ) {}

    public static RegionPriceMapResponse empty() {
        return new RegionPriceMapResponse(null, null, List.of());
    }
}
