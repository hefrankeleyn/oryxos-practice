package com.oryxlabs.oryxos.provider;
import com.sun.net.httpserver.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
class DeepSeekProtocolTest {
 @Test void compatibleRequestChangesOnlyThinkingAndCallsHttpOnce() throws Exception {
  var calls=new AtomicInteger();var body=new AtomicReference<String>();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",e->{calls.incrementAndGet();body.set(new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
   byte[] out="{\"id\":\"r\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}".getBytes();
   e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,out.length);e.getResponseBody().write(out);e.close();});server.start();
  try {
   var d=new ProviderDefinition("deepseek",URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"fixture-key",Duration.ofSeconds(1),Duration.ofSeconds(2));
   var models=new ProviderModelConfiguration().create(Map.of("deepseek",d));
   assertThat(new SpringAiProviderService(models).call(ProviderSelectionTest.request("deepseek")).text()).isEqualTo("ok");
   var json=new ObjectMapper().readTree(body.get());assertThat(json.at("/thinking/type").asText()).isEqualTo("disabled");
   assertThat(json.get("model").asText()).isEqualTo("model");assertThat(json.at("/messages/0/role").asText()).isEqualTo("system");assertThat(calls.get()).isEqualTo(1);
  }finally{server.stop(0);}
 }
 @Test void serverFailureIsNotRetried() throws Exception {
  var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",e->{count.incrementAndGet();e.sendResponseHeaders(503,-1);e.close();});server.start();
  try {
   var d=new ProviderDefinition("deepseek",URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"fixture",Duration.ofSeconds(1),Duration.ofSeconds(1));
   var service=new SpringAiProviderService(new ProviderModelConfiguration().create(Map.of("deepseek",d)));
   assertThatThrownBy(()->service.call(ProviderSelectionTest.request("deepseek"))).isInstanceOf(RuntimeFailure.class);assertThat(count.get()).isEqualTo(1);
  }finally{server.stop(0);}
 }
 @Test void readTimeoutIsFiniteAndNoRetryOccurs()throws Exception {
  var count=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",e->{count.incrementAndGet();try{Thread.sleep(300);}catch(InterruptedException x){Thread.currentThread().interrupt();}e.close();});server.start();
  try{
   var d=new ProviderDefinition("deepseek",URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"fixture",Duration.ofSeconds(1),Duration.ofMillis(60));
   var service=new SpringAiProviderService(new ProviderModelConfiguration().create(Map.of("deepseek",d)));
   long start=System.nanoTime();assertThatThrownBy(()->service.call(ProviderSelectionTest.request("deepseek"))).isInstanceOf(RuntimeFailure.class);
   assertThat(Duration.ofNanos(System.nanoTime()-start)).isLessThan(Duration.ofSeconds(5));assertThat(count.get()).isEqualTo(1);
  }finally{server.stop(0);}
 }
 @Test void schemaAndToolMessagesPreserveIdsAndSdkNeverExecutesCallback()throws Exception {
  var actual=new AtomicReference<com.fasterxml.jackson.databind.JsonNode>();var executions=new AtomicInteger();var requests=new AtomicInteger();
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",e->{byte[] input=e.getRequestBody().readAllBytes();requests.incrementAndGet();
   assertThat(Integer.parseInt(e.getRequestHeaders().getFirst("Content-Length"))).isEqualTo(input.length);actual.set(new ObjectMapper().readTree(input));
   byte[] out="{\"id\":\"r\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"next-id\",\"type\":\"function\",\"function\":{\"name\":\"calculate\",\"arguments\":\"{}\"}}]},\"finish_reason\":\"tool_calls\"}]}".getBytes();
   e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,out.length);e.getResponseBody().write(out);e.close();});server.start();
  try {
   var tool=new com.oryxlabs.oryxos.core.tool.OryxTool(){public String name(){return "calculate";}public String description(){return "fixture";}public String inputSchema(){return "{\"type\":\"object\",\"properties\":{}}";}public String execute(String in){executions.incrementAndGet();return "{}";}};
   var r=ProviderSelectionTest.request("deepseek");var turn=UUID.randomUUID();var call=UUID.randomUUID();
   var prior=new com.oryxlabs.oryxos.core.model.Message(UUID.randomUUID(),turn,0,com.oryxlabs.oryxos.core.model.Message.Role.ASSISTANT,"",call,List.of(new ProviderResponse.ToolCall("original-id","calculate","{}")),null,null);
   var result=new com.oryxlabs.oryxos.core.model.Message(UUID.randomUUID(),turn,1,com.oryxlabs.oryxos.core.model.Message.Role.TOOL,"2",call,List.of(),"original-id","calculate");
   r=new ProviderRequest(r.callId(),r.sessionId(),r.runId(),1,r.profile(),"system",List.of(prior,result),List.of(tool));
   var d=new ProviderDefinition("deepseek",URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"fixture",Duration.ofSeconds(1),Duration.ofSeconds(3));
   var response=new SpringAiProviderService(new ProviderModelConfiguration().create(Map.of("deepseek",d))).call(r);
   assertThat(response.toolCalls().getFirst().id()).isEqualTo("next-id");assertThat(executions.get()).isZero();assertThat(requests.get()).isEqualTo(1);
   assertThat(actual.get().at("/messages/1/tool_calls/0/id").asText()).isEqualTo("original-id");
   assertThat(actual.get().at("/messages/2/tool_call_id").asText()).isEqualTo("original-id");
   assertThat(actual.get().at("/tools/0/function/name").asText()).isEqualTo("calculate");
  }finally{server.stop(0);}
 }
}
