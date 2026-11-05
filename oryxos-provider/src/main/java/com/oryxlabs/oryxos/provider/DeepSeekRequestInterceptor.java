package com.oryxlabs.oryxos.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.http.client.*;

/** 仅安装在显式 DeepSeek 实例；保持 SDK 消息与工具字段，仅补非推理扩展参数。 */
public final class DeepSeekRequestInterceptor implements ClientHttpRequestInterceptor {
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public ClientHttpResponse intercept(
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (request.getMethod() == HttpMethod.POST
        && request.getURI().getPath().endsWith("/chat/completions")) {
      ObjectNode json = (ObjectNode) mapper.readTree(body);
      json.putObject("thinking").put("type", "disabled");
      body = mapper.writeValueAsBytes(json);
      request.getHeaders().setContentLength(body.length);
    }
    return execution.execute(request, body);
  }
}
