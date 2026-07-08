package com.todongsan.insightreputation.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.todongsan.insightreputation.insight.repository.InsightReportRepository;
import com.todongsan.insightreputation.reputation.repository.ReputationRepository;
import com.todongsan.insightreputation.visitcertification.repository.VisitCertificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class IntegrationTestBase {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ReputationRepository reputationRepository;

    @Autowired
    protected VisitCertificationRepository visitCertificationRepository;

    @Autowired
    protected InsightReportRepository insightReportRepository;

    protected static WireMockServer wireMockServer;

    @BeforeAll
    static void setUpWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        
        // Update system properties for test configuration
        System.setProperty("client.battle.base-url", "http://localhost:" + wireMockServer.port());
        System.setProperty("client.member-point.base-url", "http://localhost:" + wireMockServer.port());
        System.setProperty("claude.api.url", "http://localhost:" + wireMockServer.port() + "/claude");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        // 각 테스트 시작 전에 깨끗한 상태 보장
        insightReportRepository.deleteAll();
        visitCertificationRepository.deleteAll();
        reputationRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        // Clean up repositories for each test
        insightReportRepository.deleteAll();
        visitCertificationRepository.deleteAll();
        reputationRepository.deleteAll();
    }

    // WireMock stub helpers

    protected void stubBattleGetComment(Long commentId, Long battleId, String sido, String sigu) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/battles/comments/" + commentId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "commentId": %d,
                                "battleId": %d,
                                "memberId": 1,
                                "content": "Test comment",
                                "sido": "%s",
                                "sigu": "%s",
                                "createdAt": "2024-01-01T10:00:00"
                            }
                        }
                        """, commentId, battleId, sido, sigu))));
    }

    protected void stubBattleGetCommentNotFound(Long commentId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/battles/comments/" + commentId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                            "success": false,
                            "errorCode": "RESOURCE_NOT_FOUND",
                            "message": "댓글을 찾을 수 없습니다."
                        }
                        """)));
    }

    protected void stubBattleClosed(Long battleId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/battles/" + battleId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "battleId": %d,
                                "title": "Test Battle",
                                "sido": "서울",
                                "sigu": "성동구",
                                "status": "CLOSED",
                                "isClosed": true,
                                "optionA": "Option A",
                                "optionB": "Option B"
                            }
                        }
                        """, battleId))));
    }

    protected void stubBattleNotClosed(Long battleId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/battles/" + battleId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "battleId": %d,
                                "title": "Test Battle",
                                "sido": "서울",
                                "sigu": "성동구",
                                "status": "ACTIVE",
                                "isClosed": false,
                                "optionA": "Option A",
                                "optionB": "Option B"
                            }
                        }
                        """, battleId))));
    }

    protected void stubBattleVotesRaw(Long battleId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/v1/battles/" + battleId + "/votes/raw"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "battleId": %d,
                                "totalVotes": 2,
                                "votes": [
                                    {
                                        "memberId": 1,
                                        "selectedOption": "A",
                                        "votedAt": "2024-01-01T10:00:00"
                                    },
                                    {
                                        "memberId": 2,
                                        "selectedOption": "B",
                                        "votedAt": "2024-01-01T11:00:00"
                                    }
                                ]
                            }
                        }
                        """, battleId))));
    }

    protected void stubPointSpendSuccess(Long memberId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/points/spend"))
                .withRequestBody(containing("\"memberId\":" + memberId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "memberId": %d,
                                "amount": 80,
                                "remainingPoints": 920
                            }
                        }
                        """, memberId))));
    }

    protected void stubPointSpendFail(Long memberId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/points/spend"))
                .withRequestBody(containing("\"memberId\":" + memberId))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                            "success": false,
                            "errorCode": "POINT_INSUFFICIENT",
                            "message": "포인트가 부족합니다."
                        }
                        """)));
    }

    protected void stubPointRefund(Long memberId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/points/refund"))
                .withRequestBody(containing("\"memberId\":" + memberId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "success": true,
                            "data": {
                                "memberId": %d,
                                "refundAmount": 80
                            }
                        }
                        """, memberId))));
    }

    protected void stubMemberBatchInfo() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/members/batch"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                            "success": true,
                            "data": [
                                {
                                    "memberId": 1,
                                    "ageGroup": "20s",
                                    "gender": "MALE",
                                    "residenceSido": "서울",
                                    "residenceSigu": "성동구"
                                },
                                {
                                    "memberId": 2,
                                    "ageGroup": "30s",
                                    "gender": "FEMALE",
                                    "residenceSido": "서울",
                                    "residenceSigu": "마포구"
                                }
                            ]
                        }
                        """)));
    }

    protected void stubClaudeApiSuccess(String summary) {
        wireMockServer.stubFor(post(urlEqualTo("/claude"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.format("""
                        {
                            "content": [
                                {
                                    "text": "%s"
                                }
                            ]
                        }
                        """, summary))));
    }

    protected void stubClaudeApiFail() {
        wireMockServer.stubFor(post(urlEqualTo("/claude"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                        {
                            "error": {
                                "message": "Internal server error"
                            }
                        }
                        """)));
    }

    // Helper methods for creating test data

    protected HttpHeaders createMemberHeaders(Long memberId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Member-Id", String.valueOf(memberId));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    protected HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}