package com.todongsan.marketservice.market.controller;

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
                .andExpect(jsonPath("$.paths['/api/v1/internal/markets/predictions/reconcile']").exists())
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-summary']").exists())
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/admin/markets'].post.parameters[0].name")
                        .value("X-Member-Role"))
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions'].get.parameters[1].schema.default")
                        .value("0"))
                .andExpect(jsonPath("$.paths['/internal/api/v1/markets/{marketId}/insight-predictions'].get.parameters[2].schema.default")
                        .value("500"));
    }
}
