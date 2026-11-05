package com.oryxlabs.oryxos.web;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import com.oryxlabs.oryxos.core.runtime.SafeErrorMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** 统一处理业务路径的失败；框架与第三方端点继续使用各自的错误协议。 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Object> handleApi(ApiException failure, HttpServletRequest request) {
    if (!ApiPaths.matches(request)) {
      throw failure;
    }
    return error(failure.error().status(), new HttpHeaders());
  }

  @ExceptionHandler(RuntimeFailure.class)
  public ResponseEntity<Object> handleRuntime(RuntimeFailure failure, HttpServletRequest request) {
    if (!ApiPaths.matches(request)) {
      throw failure;
    }
    HttpStatus status =
        switch (failure.code()) {
          case PROVIDER -> HttpStatus.SERVICE_UNAVAILABLE;
          case TOOL_VALIDATION -> HttpStatus.BAD_REQUEST;
          case SANDBOX_DENIED -> HttpStatus.FORBIDDEN;
          default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    String code =
        failure.code() == RuntimeFailure.Code.PROVIDER
            ? ApiError.PROVIDER_UNAVAILABLE.name()
            : failure.code().name();
    log.warn("api_failure code={}", code);
    return ResponseEntity.status(status)
        .body(ApiResponse.failure(code, SafeErrorMapper.message(failure.code())));
  }

  @ExceptionHandler({TimeoutException.class, ConstraintViolationException.class})
  public ResponseEntity<Object> handleBoundary(Exception failure, HttpServletRequest request)
      throws Exception {
    if (!ApiPaths.matches(request)) {
      throw failure;
    }
    return error(
        failure instanceof TimeoutException ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_REQUEST,
        new HttpHeaders());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleUnexpected(Exception failure, HttpServletRequest request)
      throws Exception {
    if (!ApiPaths.matches(request)) {
      throw failure;
    }
    return error(HttpStatus.INTERNAL_SERVER_ERROR, new HttpHeaders());
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception failure,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (request instanceof ServletWebRequest servlet && ApiPaths.matches(servlet.getRequest())) {
      return error(status, headers);
    }
    return super.handleExceptionInternal(failure, body, headers, status, request);
  }

  @Override
  protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
      AsyncRequestTimeoutException failure,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    if (request instanceof ServletWebRequest servlet && ApiPaths.matches(servlet.getRequest())) {
      return error(HttpStatus.GATEWAY_TIMEOUT, headers);
    }
    return super.handleAsyncRequestTimeoutException(failure, headers, status, request);
  }

  private ResponseEntity<Object> error(HttpStatusCode status, HttpHeaders headers) {
    HttpStatus known = HttpStatus.resolve(status.value());
    String code =
        switch (status.value()) {
          case 400 -> ApiError.BAD_REQUEST.name();
          case 404 -> ApiError.NOT_FOUND.name();
          case 500 -> ApiError.INTERNAL_ERROR.name();
          case 503 -> ApiError.PROVIDER_UNAVAILABLE.name();
          case 504 -> ApiError.GATEWAY_TIMEOUT.name();
          default -> "HTTP_" + status.value();
        };
    log.warn("api_failure status={}", status.value());
    return new ResponseEntity<>(
        ApiResponse.failure(code, known == null ? "Request failed" : known.getReasonPhrase()),
        headers,
        status);
  }
}
