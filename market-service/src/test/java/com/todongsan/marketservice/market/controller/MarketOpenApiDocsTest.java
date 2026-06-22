package com.todongsan.marketservice.market.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todongsan.marketservice.market.client.MemberPointClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MarketOpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberPointClient memberPointClient;

    @Test
    void openApiDocsExposeCurrentMarketApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Todongsan Market Service API"))
                .andExpect(jsonPath("$.paths['/api/v1/markets/{marketId}/predictions/quote']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/markets/{marketId}/price-history']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}/activate']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/problem-markets']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}/settlements']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}/settlements/{settlementId}/details']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}/refunds']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/{marketId}/refunds/{voidId}/details']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/status-counts']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/internal/markets/predictions/reconcile']").exists())
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-summary']").exists())
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets'].post.parameters[*].name")
                        .value(hasItem("X-Member-Role")))
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets'].post.parameters[*].in")
                        .value(hasItem("header")))
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets/problem-markets'].get.parameters[*].name")
                        .value(hasItem("X-Member-Role")))
                .andExpect(jsonPath("$.paths['/api/v1/markets/{marketId}/predictions'].post.parameters[*].name")
                        .value(hasItem("X-Member-Id")))
                .andExpect(jsonPath("$.paths['/api/v1/markets/{marketId}/predictions'].post.parameters[*].name")
                        .value(hasItem("Idempotency-Key")))
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions'].get.parameters[*].name")
                        .value(hasItem("page")))
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions'].get.parameters[*].name")
                        .value(hasItem("size")))
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions'].get.parameters[*].in")
                        .value(hasItem("query")))
                .andExpect(jsonPath("$.components.schemas.AdminMarketSettlementDetailPageResponse.properties.content.items['$ref']")
                        .value("#/components/schemas/AdminSettlementDetailResponse"))
                .andExpect(jsonPath("$.components.schemas.AdminMarketRefundDetailPageResponse.properties.content.items['$ref']")
                        .value("#/components/schemas/AdminRefundDetailResponse"))
                .andExpect(jsonPath("$.components.schemas.AdminSettlementDetailResponse.properties.pointAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminSettlementDetailResponse.properties.contractQuantity.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminSettlementDetailResponse.properties.settledAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminSettlementDetailResponse.properties.profitAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminRefundDetailResponse.properties.pointAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminRefundDetailResponse.properties.refundAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminRefundDetailResponse.properties.settlementId").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AdminRefundDetailResponse.properties.settledAmount").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AdminRefundDetailResponse.properties.profitAmount").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.AdminMarketSettlementResponse.properties.totalPool.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminMarketRefundResponse.properties.totalRefundAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminMarketDetailResponse.properties.totalRealPoolAmount.type")
                        .value("string"))
                .andExpect(jsonPath("$.components.schemas.AdminMarketOption.properties.currentPrice.type")
                        .value("string"));
    }
}
