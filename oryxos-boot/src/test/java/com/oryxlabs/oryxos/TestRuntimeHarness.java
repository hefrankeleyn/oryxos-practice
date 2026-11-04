package com.oryxlabs.oryxos;
import com.oryxlabs.oryxos.core.agent.*;
import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.memory.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.*;
import com.oryxlabs.oryxos.core.tool.*;
import com.oryxlabs.oryxos.memory.*;
import com.oryxlabs.oryxos.tool.*;
import com.oryxlabs.oryxos.fixtures.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.UnaryOperator;
/** 真实 Runtime 和 SQLite 的隔离测试装配；仅 Provider 响应与审计故障可受控替换。 */
final class TestRuntimeHarness implements AutoCloseable {
 final TestAuditRuntime storage;
 final PureCalculationTool calculate=new PureCalculationTool();final DeniedActionTool denied=new DeniedActionTool();
 final MemoryService memory=new DefaultMemoryService(new InMemorySessionManager());
 final Session session=memory.session(new SessionKey("test","fixture","demo"));
 final AgentService agent;
 TestRuntimeHarness(Path home,int iterations,ProviderService provider)throws Exception{this(home,iterations,provider,a->a);}
 TestRuntimeHarness(Path home,int iterations,ProviderService provider,UnaryOperator<AuditStore> fault)throws Exception{this(home,iterations,provider,fault,"fixture","model");}
 TestRuntimeHarness(Path home,int iterations,ProviderService provider,UnaryOperator<AuditStore> fault,String providerName,String model)throws Exception{
  Files.createDirectories(home.resolve("agents/demo"));
  Files.writeString(home.resolve("agents/demo/AGENT.md"),"---\nname: demo\nprovider: {name: "+providerName+", model: "+new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(model)+"}\ntools: "+(providerName.equals("deepseek")?"[calculate]":"[calculate, denied_action, fail_action]")+"\nsettings: {max_iterations: "+iterations+"}\n---\nUse supplied tools when requested.");
  storage=new TestAuditRuntime(home);AuditStore audit=fault.apply(storage.audit);
  OryxTool fail=new OryxTool(){public String name(){return "fail_action";}public String description(){return "Failure fixture";}public String inputSchema(){return "{\"type\":\"object\",\"properties\":{}}";}public String execute(String args){throw new IllegalStateException("fixture-sensitive-error");}};
  var registry=new ToolRegistry(providerName.equals("deepseek")?List.of(calculate):List.of(calculate,denied,fail));var invoker=new DefaultToolInvoker(registry,new WhitelistSandbox(List.of(home),Set.of(),Set.of()),new ToolInputValidator());
  var definition=new AgentLoader(Set.of(providerName),registry.names(),n->null).load(home,"demo");
  var loop=new ReActLoop(new AuditedProviderService(provider,audit),new PromptBuilder(memory,new ContextLoader(),registry),memory,new ToolExecutor(invoker,audit,new ExecutionReceiptStore()));
  agent=new AgentService(new ProfileRegistry(List.of(definition)),memory,loop);
 }
 static ProviderResponse tools(ProviderResponse.ToolCall... calls){return new ProviderResponse("",List.of(calls),null,null,null);}
 static ProviderResponse done(){return new ProviderResponse("done",List.of(),null,null,null);}
 static ProviderResponse.ToolCall add(String id){return new ProviderResponse.ToolCall(id,"calculate","{\"a\":1,\"b\":2}");}
 @Override public void close(){storage.close();}
}
