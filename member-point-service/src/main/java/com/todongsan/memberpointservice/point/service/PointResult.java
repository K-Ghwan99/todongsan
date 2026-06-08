package com.todongsan.memberpointservice.point.service;

public record PointResult<T>(T data, boolean alreadyProcessed) {

    public static <T> PointResult<T> of(T data) {
        return new PointResult<>(data, false);
    }

    public static <T> PointResult<T> alreadyProcessed(T data) {
        return new PointResult<>(data, true);
    }
}
