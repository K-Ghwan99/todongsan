package com.todongsan.apigateway.global.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtAuthenticationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(SECRET);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void 공개경로_카카오로그인_JWT검증없이통과() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/members/oauth/kakao").build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void 공개경로_토큰재발급_JWT검증없이통과() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/members/token/refresh").build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void 유효한토큰_헤더추가후통과() {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "USER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SECRET_KEY)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void 유효한토큰_스푸핑헤더제거후JWT기준헤더추가() {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "USER")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SECRET_KEY)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-Member-Id", "999")
                        .header("X-Member-Role", "ADMIN")
                        .build()
        );

        // chain.filter에 전달된 exchange의 request 헤더를 검증하기 위해 캡처
        ServerWebExchange[] capturedExchange = new ServerWebExchange[1];
        when(chain.filter(any())).thenAnswer(invocation -> {
            capturedExchange[0] = invocation.getArgument(0);
            return Mono.empty();
        });

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        HttpHeaders headers = capturedExchange[0].getRequest().getHeaders();
        assertThat(headers.getFirst("X-Member-Id")).isEqualTo("42");
        assertThat(headers.getFirst("X-Member-Role")).isEqualTo("USER");
    }

    @Test
    void 토큰없음_401반환() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me").build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 만료된토큰_401반환() {
        String token = Jwts.builder()
                .subject("42")
                .claim("role", "USER")
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(SECRET_KEY)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void 위조된토큰_401반환() {
        String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5OTkifQ.fake-signature";

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeToken)
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void Bearer_접두사없음_401반환() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "invalid-token")
                        .build()
        );

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
