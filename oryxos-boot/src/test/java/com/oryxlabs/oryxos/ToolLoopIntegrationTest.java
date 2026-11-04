package com.oryxlabs.oryxos;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
class ToolLoopIntegrationTest {
 @TempDir Path home;
 @Test void sequentialBatchThenIndependentSameArgumentsRemainPaired()throws Exception{
  var calls=new AtomicInteger();
  try(var h=new TestRuntimeHarness(home,10,r->{
   int index=calls.getAndIncrement();if(index==0)return TestRuntimeHarness.tools(TestRuntimeHarness.add("id1"),TestRuntimeHarness.add("id2"));
   if(index==1){assertThat(r.messages().stream().filter(m->m.role()==Message.Role.TOOL).map(Message::toolCallId)).containsExactly("id1","id2");return TestRuntimeHarness.tools(TestRuntimeHarness.add("id1"));}
   assertThat(r.messages().stream().filter(m->m.role()==Message.Role.TOOL)).hasSize(3);return TestRuntimeHarness.done();
  })){
   assertThat(h.agent.process(h.session,"calculate")).isEqualTo("done");assertThat(h.calculate.executions.get()).isEqualTo(3);assertThat(calls.get()).isEqualTo(3);
   assertThat(h.storage.jdbc.queryForObject("select count(*) from tool_invocations",Integer.class)).isEqualTo(3);assertThat(com.oryxlabs.oryxos.core.agent.ProfileContext.get()).isNull();
  }
 }
 @Test void duplicateWireIdsRejectWholeBatchWithoutExecuting()throws Exception{
  try(var h=new TestRuntimeHarness(home,10,r->TestRuntimeHarness.tools(TestRuntimeHarness.add("same"),TestRuntimeHarness.add("same")))){
   assertThatThrownBy(()->h.agent.process(h.session,"calculate")).isInstanceOf(RuntimeFailure.class).hasMessage("PROTOCOL");assertThat(h.calculate.executions.get()).isZero();
   assertThat(h.storage.jdbc.queryForObject("select count(*) from tool_invocations",Integer.class)).isZero();
  }
 }
 @Test void lastAllowedModelIterationNeverExecutesUnreturnableTool()throws Exception{
  var calls=new AtomicInteger();try(var h=new TestRuntimeHarness(home,2,r->{calls.incrementAndGet();return TestRuntimeHarness.tools(TestRuntimeHarness.add("id"));})){
   assertThatThrownBy(()->h.agent.process(h.session,"calculate")).hasMessage("ITERATION_LIMIT");assertThat(calls.get()).isEqualTo(2);assertThat(h.calculate.executions.get()).isEqualTo(1);
   assertThat(h.session.turns().getFirst().state()).isEqualTo(Turn.State.LIMIT_REACHED);
  }
 }
 @Test void invalidUnknownAndSandboxDeniedRequestsReturnAuditedFailure()throws Exception{
  var requests=List.of(new ProviderResponse.ToolCall("id","missing","{}"),new ProviderResponse.ToolCall("id","calculate","{\"a\":\"bad\",\"b\":2}"),new ProviderResponse.ToolCall("id","denied_action","{}"));
  for(int i=0;i<requests.size();i++){
   var calls=new AtomicInteger();var tool=requests.get(i);
   try(var h=new TestRuntimeHarness(home.resolve("case"+i),10,r->{if(calls.getAndIncrement()==0)return TestRuntimeHarness.tools(tool);assertThat(r.messages().getLast().role()).isEqualTo(Message.Role.TOOL);return TestRuntimeHarness.done();})){
    h.agent.process(h.session,"request");assertThat(h.calculate.executions.get()).isZero();assertThat(h.denied.executions.get()).isZero();
    assertThat(h.storage.jdbc.queryForMap("select status,executed from tool_invocations")).containsEntry("status","REJECTED").containsEntry("executed",0);
   }
  }
 }
}
