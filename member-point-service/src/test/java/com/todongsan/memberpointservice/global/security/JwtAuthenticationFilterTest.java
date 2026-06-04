package com.todongsan.memberpointservice.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.exception.ErrorCode;
import com.todongsan.memberpointservice.member.entity.MemberRole;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock JwtProvider jwtProvider;
    @Mock FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        filter = new JwtAuthenticationFilter(jwtProvider, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰_SecurityContext_설정() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtProvider.extractMemberId("valid-token")).thenReturn(1L);
        when(jwtProvider.extractRole("valid-token")).thenReturn(MemberRole.USER);

        filter.doFilterInternal(request, response, filterChain);

        MemberPrincipal principal = (MemberPrincipal)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getMemberId()).isEqualTo(1L);
        assertThat(principal.getRole()).isEqualTo(MemberRole.USER);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰_없으면_SecurityContext_비어있고_통과() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 유효하지_않은_토큰_401_반환_필터체인_중단() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new CustomException(ErrorCode.INVALID_TOKEN))
                .when(jwtProvider).validateToken("invalid-token");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void 만료된_토큰_401_TOKEN_EXPIRED() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expired-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        doThrow(new CustomException(ErrorCode.TOKEN_EXPIRED))
                .when(jwtProvider).validateToken("expired-token");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
