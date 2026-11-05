package com.oryxlabs.oryxos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    classes = WebContractTest.TestApp.class,
    properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class WebContractTest {
  private static final String PRIVATE_VALUE = "fixture-private-prompt-value";
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;

  @Test
  void successAndValidationShareEnvelopeWithoutEchoingInvalidInput(CapturedOutput logs)
      throws Exception {
    mvc.perform(
            post("/api/v1/fixture")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new Payload("hello"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.message").value("hello"))
        .andExpect(jsonPath("$.timestamp").isString());
    String invalid = PRIVATE_VALUE.repeat(2000);
    String response =
        mvc.perform(
                post("/api/v1/fixture")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(new Payload(invalid))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.data").doesNotExist())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain(PRIVATE_VALUE, "stackTrace");
    assertThat(logs.getAll()).doesNotContain(PRIVATE_VALUE);
  }

  @Test
  void utf8ByteBoundaryIncludesMultibyteCharacters() throws Exception {
    for (String valid :
        new String[] {"a".repeat(32768), "中".repeat(10922) + "ab", "😀".repeat(8192)}) {
      mvc.perform(
              post("/api/v1/fixture")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(new Payload(valid))))
          .andExpect(status().isOk());
      mvc.perform(
              post("/api/v1/fixture")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsString(new Payload(valid + "x"))))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  void errorsKeepStatusAndNeverRevealOriginalException(CapturedOutput logs) throws Exception {
    Map<String, Integer> errors =
        Map.of("missing", 404, "unexpected", 500, "provider", 503, "timeout", 504);
    for (var entry : errors.entrySet()) {
      String response =
          mvc.perform(get("/api/v1/fail/" + entry.getKey()))
              .andExpect(status().is(entry.getValue()))
              .andExpect(jsonPath("$.code").isString())
              .andExpect(jsonPath("$.message").isString())
              .andExpect(jsonPath("$.timestamp").isString())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertThat(response).doesNotContain(PRIVATE_VALUE, "stackTrace", "exception");
    }
    mvc.perform(get("/api/v1/does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    mvc.perform(post("/api/v1/fixture").contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    assertThat(logs.getAll()).doesNotContain(PRIVATE_VALUE);
  }

  @Test
  void requestContextIsFreshAndInfrastructureResponsesAreNotWrapped() throws Exception {
    String first =
        mvc.perform(get("/api/v1/context")).andReturn().getResponse().getContentAsString();
    String second =
        mvc.perform(get("/api/v1/context")).andReturn().getResponse().getContentAsString();
    assertThat(json.readTree(first).path("traceId").asText())
        .isNotBlank()
        .isNotEqualTo(json.readTree(second).path("traceId").asText());
    assertThat(MDC.get("traceId")).isNull();
    assertThat(
            mvc.perform(get("/infrastructure/ping")).andReturn().getResponse().getContentAsString())
        .isEqualTo("plain");
  }

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import({FixtureController.class, GlobalExceptionHandler.class, RequestLoggingFilter.class})
  static class TestApp {}

  public record Payload(@NotBlank @Utf8Length String message) {}

  @RestController
  static class FixtureController {
    @PostMapping("/api/v1/fixture")
    ApiResponse<Payload> echo(@Valid @RequestBody Payload payload) {
      return ApiResponse.success(payload);
    }

    @GetMapping("/api/v1/fail/{failure}")
    Object fail(@PathVariable String failure) throws TimeoutException {
      switch (failure) {
        case "missing" -> throw new ApiException(ApiError.NOT_FOUND);
        case "provider" -> throw new RuntimeFailure(RuntimeFailure.Code.PROVIDER);
        case "timeout" -> throw new TimeoutException(PRIVATE_VALUE);
        default -> throw new IllegalStateException(PRIVATE_VALUE);
      }
    }

    @GetMapping("/api/v1/context")
    Map<String, String> context() {
      return Map.of("traceId", MDC.get("traceId"));
    }

    @GetMapping("/infrastructure/ping")
    String infrastructure() {
      return "plain";
    }
  }
}
