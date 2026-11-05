package com.oryxlabs.oryxos.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 关闭文档 UI 时一并阻断 WebJar 静态入口，避免绕过 springdoc 的入口开关。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    name = "springdoc.swagger-ui.enabled",
    havingValue = "false",
    matchIfMissing = true)
public final class DocumentationAccessFilter extends OncePerRequestFilter {
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = ApiPaths.path(request);
    return !(path.equals("/swagger-ui.html")
        || path.equals("/swagger-ui")
        || path.startsWith("/swagger-ui/")
        || path.equals("/webjars/swagger-ui")
        || path.startsWith("/webjars/swagger-ui/"));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }
}
