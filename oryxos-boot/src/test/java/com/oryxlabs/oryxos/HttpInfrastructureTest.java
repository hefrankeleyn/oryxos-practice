package com.oryxlabs.oryxos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.servlet.function.RequestPredicates.GET;
import static org.springframework.web.servlet.function.RouterFunctions.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/** 在真实回环 HTTP 和临时 SQLite 上验证基础设施，不访问模型。 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.address=127.0.0.1", "management.server.port=0"})
@Import(HttpInfrastructureTest.ProbeConfiguration.class)
@AutoConfigureObservability
class HttpInfrastructureTest {
  @TempDir static Path home;
  @Autowired TestRestTemplate http;
  @Autowired ObjectMapper json;
  @LocalManagementPort int managementPort;

  @DynamicPropertySource
  static void isolatedWorkspace(DynamicPropertyRegistry properties) {
    properties.add("oryxos.home", home::toString);
    properties.add("spring.datasource.url", () -> "jdbc:sqlite:" + home.resolve("oryxos.db"));
  }

  @Test
  void onlyManagementPortExposesBoundedMonitoring() throws Exception {
    String management = "http://127.0.0.1:" + managementPort;
    var health = http.getForEntity(management + "/actuator/health", String.class);
    assertThat(health.getStatusCode().value()).isEqualTo(200);
    var healthJson = json.readTree(health.getBody());
    assertThat(healthJson.path("status").asText()).isEqualTo("UP");
    assertThat(healthJson.fieldNames()).toIterable().containsExactlyInAnyOrder("status", "groups");
    assertThat(healthJson.path("groups").toString()).contains("liveness", "readiness");
    var prometheus = http.getForEntity(management + "/actuator/prometheus", String.class);
    assertThat(prometheus.getStatusCode().value()).isEqualTo(200);
    assertThat(prometheus.getBody()).contains("jvm_memory_used_bytes").doesNotContain("\"code\"");
    for (String path :
        new String[] {"/actuator/env", "/actuator/configprops", "/actuator/metrics"}) {
      assertThat(http.getForEntity(management + path, String.class).getStatusCode().value())
          .isEqualTo(404);
    }
    assertThat(http.getForEntity("/actuator/health", String.class).getStatusCode().value())
        .isEqualTo(404);
  }

  @Test
  void servletUsesVirtualThreadsAndIndependentRequestContexts() throws Exception {
    var first = json.readTree(http.getForObject("/api/v1/_test/thread", String.class));
    var second = json.readTree(http.getForObject("/api/v1/_test/thread", String.class));
    assertThat(first.path("virtual").asBoolean()).isTrue();
    assertThat(first.path("traceId").asText())
        .isNotBlank()
        .isNotEqualTo(second.path("traceId").asText());
  }

  @Test
  void documentationIsDisabledByDefault() {
    for (String path :
        new String[] {
          "/v3/api-docs",
          "/swagger-ui.html",
          "/swagger-ui/index.html",
          "/webjars/swagger-ui/index.html",
          "/webjars/swagger-ui/5.32.2/index.html",
          "/webjars/%73wagger-ui/5.32.2/index.html"
        }) {
      assertThat(
              http.getForEntity(java.net.URI.create(http.getRootUri() + path), String.class)
                  .getStatusCode()
                  .value())
          .isEqualTo(404);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ProbeConfiguration {
    @Bean
    RouterFunction<ServerResponse> threadProbe() {
      return route(
          GET("/api/v1/_test/thread"),
          request ->
              ServerResponse.ok()
                  .body(
                      Map.of(
                          "virtual",
                          Thread.currentThread().isVirtual(),
                          "traceId",
                          MDC.get("traceId"))));
    }
  }
}
