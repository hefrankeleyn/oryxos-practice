package com.oryxlabs.oryxos;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.*;
import com.oryxlabs.oryxos.provider.SpringAiProviderService;
import com.oryxlabs.oryxos.channel.cli.CliChannel;
import org.springframework.ai.chat.model.ChatModel;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
@ExtendWith(OutputCaptureExtension.class)
class SensitiveDataBoundaryTest {
 @TempDir Path home;
 @Test void rawProviderFailureToolArgumentsAndPromptStayOutOfTerminalLogsAndAudit(CapturedOutput capture)throws Exception{
  String secret="fixture-key-MUST-STAY-PRIVATE";String prompt="PRIVATE_FULL_PROMPT";var calls=new AtomicInteger();
  ChatModel raw=p->{throw new IllegalStateException("Authorization "+secret,new IllegalArgumentException(secret));};
  ProviderService model=r->{if(calls.getAndIncrement()==0)return TestRuntimeHarness.tools(new ProviderResponse.ToolCall("wire","calculate","{\"a\":\""+secret+"\",\"b\":2}"));return new SpringAiProviderService(Map.of("fixture",raw)).call(r);};
  var out=new StringWriter();var error=new StringWriter();
  try(var h=new TestRuntimeHarness(home,10,model)){
   assertThat(new CliChannel(h.agent,h.session).interact(new StringReader(prompt+"\n/quit\n"),new PrintWriter(out),new PrintWriter(error))).isEqualTo(1);
   String audit=h.storage.jdbc.queryForList("select * from llm_calls").toString()+h.storage.jdbc.queryForList("select * from tool_invocations").toString();
   assertThat(audit).doesNotContain(secret,prompt,"Authorization");
   assertThat(out.toString()+error).doesNotContain(secret,prompt,"Authorization");
   assertThat(error.toString()).contains("模型调用失败");
  }
  assertThat(capture.getAll()).doesNotContain(secret,prompt,"Authorization");
 }
 @Test void malformedCredentialConfigurationDoesNotRetainRawParserCause(CapturedOutput capture)throws Exception{
  String secret="fixture-parser-secret";Path file=home.resolve("runtime.yaml");Files.writeString(file,"oryxos: ["+secret);
  assertThatThrownBy(()->new com.oryxlabs.oryxos.cli.ConfigLoader().load(file,n->null)).isInstanceOf(RuntimeFailure.class).hasNoCause().hasMessage("CONFIGURATION");
  assertThat(capture.getAll()).doesNotContain(secret);
 }
 @Test void successfulToolResultIsNotSerializedIntoAuditAndCredentialEchoIsBlocked(CapturedOutput capture)throws Exception {
  String secret="fixture-tool-result-private-key";
  try(var storage=new TestAuditRuntime(home)){
   var executor=new com.oryxlabs.oryxos.core.runtime.ToolExecutor((name,args,allowed)->new com.oryxlabs.oryxos.core.tool.ToolResult(true,true,secret,null,false),storage.audit,new com.oryxlabs.oryxos.core.runtime.ExecutionReceiptStore());
   var a=new com.oryxlabs.oryxos.core.audit.InvocationAudit(UUID.randomUUID(),com.oryxlabs.oryxos.core.audit.InvocationAudit.Kind.TOOL,UUID.randomUUID(),"fixture",UUID.randomUUID(),1,10,java.time.Instant.now(),null,null,UUID.randomUUID(),"wire","calculate");
   var result=executor.execute(a,"{}",List.of("calculate"));assertThat(result.content()).isEqualTo(secret);assertThat(result.toString()).doesNotContain(secret);
   assertThat(storage.jdbc.queryForList("select * from tool_invocations").toString()).doesNotContain(secret);
   ChatModel raw=p->new org.springframework.ai.chat.model.ChatResponse(List.of(new org.springframework.ai.chat.model.Generation(new org.springframework.ai.chat.messages.AssistantMessage(secret))));
   var model=new SpringAiProviderService(Map.of("fixture",raw),List.of(secret));
   var profile=new com.oryxlabs.oryxos.core.model.ProfileSnapshot("fixture","fixture","model",null,List.of(),List.of(),10,20,"");
   var request=new ProviderRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,profile,"",List.of(),List.of());
   assertThatThrownBy(()->model.call(request)).isInstanceOf(RuntimeFailure.class).hasMessage("PROTOCOL").hasNoCause();
  }
  assertThat(capture.getAll()).doesNotContain(secret);
 }
}
