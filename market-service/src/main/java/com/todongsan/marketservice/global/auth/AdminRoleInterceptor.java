package com.todongsan.marketservice.global.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todongsan.marketservice.global.exception.errorcode.CommonErrorCode;
import com.todongsan.marketservice.global.exception.errorcode.ErrorCode;
import com.todongsan.marketservice.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AdminRoleInterceptor implements HandlerInterceptor {

    private static final String MEMBER_ROLE_HEADER = "X-Member-Role";
    private static final String ADMIN_ROLE = "ADMIN";

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod) || !hasAdminOnly(handlerMethod)) {
            return true;
        }

        String role = request.getHeader(MEMBER_ROLE_HEADER);
        if (ADMIN_ROLE.equalsIgnoreCase(role)) {
            return true;
        }

        ErrorCode errorCode = CommonErrorCode.FORBIDDEN;
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(errorCode));
        return false;
    }

    private boolean hasAdminOnly(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(AdminOnly.class)
                || handlerMethod.getBeanType().isAnnotationPresent(AdminOnly.class);
    }

}
