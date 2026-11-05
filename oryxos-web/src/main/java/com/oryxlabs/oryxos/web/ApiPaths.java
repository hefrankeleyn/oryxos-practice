package com.oryxlabs.oryxos.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

/** 将业务处理限定在版本化路径，避免修改监控、文档等协议。 */
final class ApiPaths {
  private ApiPaths() {}

  static boolean matches(HttpServletRequest request) {
    String path = path(request);
    return path.equals("/api/v1") || path.startsWith("/api/v1/");
  }

  static String path(HttpServletRequest request) {
    // 优先使用容器解码、规范化后的映射路径；MockMvc 尚未填充 servletPath 时使用框架解析。
    String servlet = request.getServletPath();
    if (servlet.isEmpty()) {
      return UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    }
    String remaining = request.getPathInfo();
    return remaining == null ? servlet : servlet + remaining;
  }
}
