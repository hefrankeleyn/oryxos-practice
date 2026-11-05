package com.oryxlabs.oryxos.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为同步业务请求建立内部关联标识；日志不记录路径、查询、请求体或原始异常。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public final class RequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !ApiPaths.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Map<String, String> previous = MDC.getCopyOfContextMap();
    long started = System.nanoTime();
    int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    MDC.put("traceId", UUID.randomUUID().toString());
    try {
      chain.doFilter(request, response);
      status = response.getStatus();
    } finally {
      log.info(
          "http_request_completed method={} status={} durationMs={}",
          logMethod(request.getMethod()),
          status,
          (System.nanoTime() - started) / 1_000_000);
      // 恢复上游上下文，同时移除本请求创建的标识，兼容虚拟线程与复用线程。
      if (previous == null) {
        MDC.clear();
      } else {
        MDC.setContextMap(previous);
      }
    }
  }

  private static String logMethod(String method) {
    // 日志只接受固定方法名；自定义方法不把客户端原文带入日志。
    return switch (method) {
      case "GET" -> "GET";
      case "POST" -> "POST";
      case "PUT" -> "PUT";
      case "PATCH" -> "PATCH";
      case "DELETE" -> "DELETE";
      case "HEAD" -> "HEAD";
      case "OPTIONS" -> "OPTIONS";
      default -> "OTHER";
    };
  }
}
