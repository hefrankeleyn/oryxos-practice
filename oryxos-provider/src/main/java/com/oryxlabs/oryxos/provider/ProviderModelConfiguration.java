package com.oryxlabs.oryxos.provider;

import com.oryxlabs.oryxos.core.provider.ProviderDefinition;
import java.net.http.HttpClient;
import java.util.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

/** 具名创建 ChatModel，不依赖 Bean 类型扫描；禁用框架工具执行和隐藏重试。 */
public final class ProviderModelConfiguration {
  public Map<String, ChatModel> create(Map<String, ProviderDefinition> definitions) {
    // JDK 在首次创建 HTTP exchange 时缓存重试策略，必须早于本 Runtime 的首个客户端请求。
    System.setProperty("jdk.httpclient.disableRetryConnect", "true");
    System.setProperty("jdk.httpclient.enableAllMethodRetry", "false");
    Map<String, ChatModel> models = new LinkedHashMap<>();
    definitions.forEach(
        (name, d) -> {
          if (!name.equals(d.name()))
            throw new com.oryxlabs.oryxos.core.runtime.RuntimeFailure(
                com.oryxlabs.oryxos.core.runtime.RuntimeFailure.Code.CONFIGURATION,
                "oryxos.providers");
          var client =
              HttpClient.newBuilder()
                  .connectTimeout(d.connectTimeout())
                  .followRedirects(HttpClient.Redirect.NEVER)
                  .build();
          var factory = new JdkClientHttpRequestFactory(client);
          factory.setReadTimeout(d.readTimeout());
          var rest = RestClient.builder().requestFactory(factory);
          if (name.equals("deepseek")) rest.requestInterceptor(new DeepSeekRequestInterceptor());
          var api =
              OpenAiApi.builder()
                  .baseUrl(d.baseUrl().toString())
                  .apiKey(d.apiKey())
                  .completionsPath("/chat/completions")
                  .restClientBuilder(rest)
                  .responseErrorHandler(
                      new org.springframework.web.client.ResponseErrorHandler() {
                        public boolean hasError(
                            org.springframework.http.client.ClientHttpResponse response)
                            throws java.io.IOException {
                          return response.getStatusCode().isError();
                        }

                        public void handleError(
                            org.springframework.http.client.ClientHttpResponse response) {
                          throw new com.oryxlabs.oryxos.core.runtime.RuntimeFailure(
                              com.oryxlabs.oryxos.core.runtime.RuntimeFailure.Code.PROVIDER);
                        }
                      })
                  .build();
          models.put(
              name,
              OpenAiChatModel.builder()
                  .openAiApi(api)
                  .defaultOptions(
                      OpenAiChatOptions.builder().internalToolExecutionEnabled(false).build())
                  .toolExecutionEligibilityPredicate((options, response) -> false)
                  .retryTemplate(RetryTemplate.builder().maxAttempts(1).noBackoff().build())
                  .build());
        });
    return Map.copyOf(models);
  }
}
