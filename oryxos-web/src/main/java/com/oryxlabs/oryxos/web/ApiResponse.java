package com.oryxlabs.oryxos.web;

import java.time.Instant;

/** 业务 API 的统一信封；错误不携带原始输入、异常或堆栈，基础设施端点保留各自协议。 */
public record ApiResponse<T>(String code, String message, T data, Instant timestamp) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("OK", "OK", data, Instant.now());
  }

  public static ApiResponse<Void> failure(String code, String message) {
    return new ApiResponse<>(code, message, null, Instant.now());
  }
}
