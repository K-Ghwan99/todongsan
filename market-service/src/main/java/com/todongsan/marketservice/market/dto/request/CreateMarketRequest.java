package com.todongsan.marketservice.market.dto.request;

import com.todongsan.marketservice.market.type.MarketAnswerType;
import com.todongsan.marketservice.market.type.MarketCategory;
import com.todongsan.marketservice.market.type.MarketMetricUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMarketRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull
    private MarketCategory category;

    @NotNull
    private MarketAnswerType answerType;

    private MarketMetricUnit metricUnit;

    @NotBlank
    @Size(max = 255)
    private String judgeDataSource;

    @NotBlank
    private String judgeCriteria;

    @NotNull
    private LocalDate judgeDate;

    @NotNull
    private LocalDateTime closeAt;

    private LocalDateTime settleDueAt;
    private BigDecimal feeRate;

    @NotNull
    private Long createdBy;

    @Valid
    @NotEmpty
    private List<CreateMarketOptionRequest> options;
}
