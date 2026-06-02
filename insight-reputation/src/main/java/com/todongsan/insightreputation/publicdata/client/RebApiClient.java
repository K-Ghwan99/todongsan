package com.todongsan.insightreputation.publicdata.client;

import com.todongsan.insightreputation.global.exception.CustomException;
import com.todongsan.insightreputation.global.exception.errorcode.ErrorCode;
import com.todongsan.insightreputation.publicdata.dto.RebApiResponse;
import com.todongsan.insightreputation.publicdata.dto.RebDataRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RebApiClient {

    private final RestTemplate restTemplate;
    
    @Value("${reb.api.key:4178fe4edf654e8dad253d7d45db2ce4}")
    private String apiKey;
    
    @Value("${reb.api.base-url:https://www.r-one.co.kr/rone/resis/common/statisticsViewer/SttsApiTblData.do}")
    private String baseUrl;
    
    // 통계표 ID
    public static final String WEEKLY_STATBL_ID = "T244183132827305";   // 주간 매매가격지수
    public static final String MONTHLY_STATBL_ID = "A_2024_00045";      // 월간 매매가격지수
    
    // 주기 코드
    public static final String WEEKLY_PERIOD = "WK";
    public static final String MONTHLY_PERIOD = "MM";

    /**
     * R-ONE API에서 가격지수 데이터 조회
     * 
     * @param statblId 통계표 ID (주간: T244183132827305, 월간: A_2024_00045)
     * @param period 주기 (WK, MM)
     * @return 가격지수 데이터 목록
     */
    public List<RebDataRow> fetchPriceIndex(String statblId, String period) {
        List<RebDataRow> allData = new ArrayList<>();
        int pageNo = 1;
        int numOfRows = 1000;  // 한 번에 최대 1000건 조회
        boolean hasMoreData = true;
        
        log.info("R-ONE API 가격지수 조회 시작: statblId={}, period={}", statblId, period);
        
        while (hasMoreData) {
            try {
                // API 호출 URL 구성
                URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("ServiceKey", apiKey)
                    .queryParam("STATBL_ID", statblId)
                    .queryParam("period", period)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .build()
                    .toUri();
                
                log.debug("R-ONE API 호출: pageNo={}, numOfRows={}", pageNo, numOfRows);
                
                RebApiResponse response = restTemplate.getForObject(uri, RebApiResponse.class);
                
                if (response == null || response.getResultInfo() == null) {
                    log.error("R-ONE API 응답 없음: statblId={}, period={}, pageNo={}", statblId, period, pageNo);
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_BAD_RESPONSE);
                }
                
                // 응답 코드 확인
                String resultCode = response.getResultInfo().getResultCode();
                if (!"00".equals(resultCode)) {
                    log.error("R-ONE API 오류 응답: resultCode={}, resultMsg={}, statblId={}, period={}", 
                             resultCode, response.getResultInfo().getResultMsg(), statblId, period);
                    throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
                }
                
                List<RebDataRow> currentPageData = response.getBody();
                if (currentPageData == null || currentPageData.isEmpty()) {
                    log.info("R-ONE API 더 이상 데이터 없음: pageNo={}", pageNo);
                    hasMoreData = false;
                } else {
                    allData.addAll(currentPageData);
                    
                    // 다음 페이지 존재 여부 확인
                    Integer totalCount = response.getResultInfo().getTotalCount();
                    int currentTotal = (pageNo - 1) * numOfRows + currentPageData.size();
                    hasMoreData = (totalCount != null && currentTotal < totalCount);
                    
                    log.debug("R-ONE API 페이지 처리 완료: pageNo={}, currentPageSize={}, totalCollected={}, hasMore={}", 
                             pageNo, currentPageData.size(), allData.size(), hasMoreData);
                    
                    pageNo++;
                }
                
            } catch (HttpClientErrorException e) {
                log.error("R-ONE API HTTP 오류: statblId={}, period={}, pageNo={}, status={}, message={}", 
                         statblId, period, pageNo, e.getStatusCode(), e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            } catch (ResourceAccessException e) {
                log.error("R-ONE API 연결 오류: statblId={}, period={}, pageNo={}, message={}", 
                         statblId, period, pageNo, e.getMessage());
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
            } catch (CustomException e) {
                // CustomException은 그대로 재전파
                throw e;
            } catch (Exception e) {
                log.error("R-ONE API 호출 중 예상치 못한 오류: statblId={}, period={}, pageNo={}", 
                         statblId, period, pageNo, e);
                throw new CustomException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        }
        
        log.info("R-ONE API 가격지수 조회 완료: statblId={}, period={}, totalRecords={}", 
                statblId, period, allData.size());
        return allData;
    }

    /**
     * 주간 매매가격지수 조회
     */
    public List<RebDataRow> fetchWeeklyPriceIndex() {
        return fetchPriceIndex(WEEKLY_STATBL_ID, WEEKLY_PERIOD);
    }

    /**
     * 월간 매매가격지수 조회
     */
    public List<RebDataRow> fetchMonthlyPriceIndex() {
        return fetchPriceIndex(MONTHLY_STATBL_ID, MONTHLY_PERIOD);
    }
}