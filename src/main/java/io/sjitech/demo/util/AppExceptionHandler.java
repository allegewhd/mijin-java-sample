package io.sjitech.demo.util;

import io.sjitech.demo.exception.AppException;
import io.sjitech.demo.exception.MijinException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Created by wang on 2016/07/28.
 */
@ControllerAdvice
public class AppExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AppExceptionHandler.class);

    @ExceptionHandler(value = {MijinException.class })
    protected ResponseEntity<Object> handleMijinError(RuntimeException ex, WebRequest request) {
        log.error("mijin error!!!", ex);

        String bodyOfResponse = "Mijin error occurred!";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(value = {AppException.class })
    protected ResponseEntity<Object> handleAppException(RuntimeException ex, WebRequest request) {
        log.error("app error!!!", ex);

        String bodyOfResponse = "Business error occurred!";
        return handleExceptionInternal(ex, bodyOfResponse,
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}