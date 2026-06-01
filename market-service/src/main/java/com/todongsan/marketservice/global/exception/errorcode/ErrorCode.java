package com.todongsan.marketservice.global.exception.errorcode;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
