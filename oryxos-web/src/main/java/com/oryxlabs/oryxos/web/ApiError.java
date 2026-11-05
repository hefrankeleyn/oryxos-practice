package com.oryxlabs.oryxos.web;

import org.springframework.http.HttpStatus;

/** 业务 API 的基础错误码与 HTTP 状态，消息使用标准状态说明而非原始异常。 */
public enum ApiError {
  BAD_REQUEST(HttpStatus.BAD_REQUEST),
  NOT_FOUND(HttpStatus.NOT_FOUND),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),
  PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE),
  GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT);

  private final HttpStatus status;

  ApiError(HttpStatus status) {
    this.status = status;
  }

  public HttpStatus status() {
    return status;
  }
}
