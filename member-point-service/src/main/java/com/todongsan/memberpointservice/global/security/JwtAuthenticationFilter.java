package com.todongsan.memberpointservice.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.memberpointservice.global.exception.CustomException;
import com.todongsan.memberpointservice.global.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 모든 요청에서 JWT 검증 후 SecurityContext 설정
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            // 토큰 없으면 인증 설정 없이 통과 (공개 경로는 SecurityConfig에서 허용)
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtProvider.validateToken(token);

            Long memberId = jwtProvider.extractMemberId(token);
            MemberPrincipal principal = new MemberPrincipal(memberId, jwtProvider.extractRole(token));

            //Spring Security 인증 객체 생성 후 Security에 저장
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.getRole().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            // 유효하지 않은 토큰 -> 즉시 401 반환
            sendErrorResponse(response, e);
        }
    }

    // Authorization: Bearer {token}에서 토큰 추출
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    // 에러 응답 직접 write (필터 레벨이라 @RestControllerAdvice 거치지 않음)
    private void sendErrorResponse(HttpServletResponse response, CustomException e) throws IOException {
        response.setStatus(e.getErrorCode().getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(e.getErrorCode()))
        );
    }

}
