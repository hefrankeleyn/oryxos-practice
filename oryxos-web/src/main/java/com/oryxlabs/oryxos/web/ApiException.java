package com.oryxlabs.oryxos.web;

import java.util.Objects;

/** 显式声明可公开的错误类别；异常原因和调用方输入不进入返回契约。 */
public final class ApiException extends RuntimeException {
  private final ApiError error;

  public ApiException(ApiError error) {
    super(Objects.requireNonNull(error).name());
    this.error = error;
  }

  public ApiError error() {
    return error;
  }
}
