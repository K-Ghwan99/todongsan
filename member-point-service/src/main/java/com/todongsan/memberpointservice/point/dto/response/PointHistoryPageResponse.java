package com.todongsan.memberpointservice.point.dto.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PointHistoryPageResponse {

    private final List<PointHistoryResponse> content;
    private final long totalElements;
    private final int totalPages;
    private final int currentPage;

    public PointHistoryPageResponse(Page<PointHistoryResponse> page) {
        this.content = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.currentPage = page.getNumber();
    }

}
