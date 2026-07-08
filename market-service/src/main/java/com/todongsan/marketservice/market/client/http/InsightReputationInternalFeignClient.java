package com.todongsan.marketservice.market.client.http;

import com.todongsan.marketservice.market.client.http.InsightReputationHttpDtos.PredictionAccuracyUpdateRequest;
import com.todongsan.marketservice.market.client.http.InsightReputationHttpDtos.PredictionAccuracyUpdateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "insight-reputation-internal",
        url = "${client.insight.base-url:http://localhost:8083}"
)
public interface InsightReputationInternalFeignClient {

    @PostMapping("/internal/api/v1/reputations/prediction")
    InsightApiResponse<PredictionAccuracyUpdateResponse> updatePredictionAccuracy(
            @RequestBody PredictionAccuracyUpdateRequest request
    );
}
