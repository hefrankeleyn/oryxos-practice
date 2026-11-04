package com.oryxlabs.oryxos;
import com.oryxlabs.oryxos.core.agent.*;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.*;
import com.oryxlabs.oryxos.core.tool.*;
import com.oryxlabs.oryxos.tool.*;
import com.oryxlabs.oryxos.provider.*;
import com.oryxlabs.oryxos.memory.*;
import com.oryxlabs.oryxos.storage.JpaAuditStore;
import com.oryxlabs.oryxos.channel.cli.CliChannel;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.util.*;
/** Boot 是唯一聚合点；轻量 CLI 不反向依赖任何功能实现。 */
@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(name="oryxos.runtime.enabled",havingValue="true")
public class RuntimeConfiguration {
 @Bean ToolRegistry toolRegistry(List<OryxTool> tools){return new ToolRegistry(tools);}
 @Bean RuntimeComponents runtimeComponents(BootRuntimeStarter.Inputs inputs,JpaAuditStore audit,ToolRegistry registry){
  var memory=new DefaultMemoryService(new InMemorySessionManager());
  var agent=new AgentLoader(inputs.providers().keySet(),registry.names(),inputs.environment()).load(inputs.home(),inputs.profile());
  new ContextLoader().read(agent);
  ToolCatalog catalog=registry;
  var adapter=new SpringAiProviderService(new ProviderModelConfiguration().create(inputs.providers()),inputs.providers().values().stream().map(ProviderDefinition::apiKey).toList());adapter.validate(agent.profile());
  var provider=new AuditedProviderService(adapter,audit);
  var executor=new ToolExecutor(new DefaultToolInvoker(registry,new WhitelistSandbox(List.of(inputs.home()),Set.of(),Set.of()),new ToolInputValidator()),audit,new ExecutionReceiptStore());
  var loop=new ReActLoop(provider,new PromptBuilder(memory,new ContextLoader(),catalog),memory,executor);
  var service=new AgentService(new ProfileRegistry(List.of(agent)),memory,loop);
  var session=memory.session(new SessionKey("cli","local",agent.profile().name()));
  return new RuntimeComponents(new CliChannel(service,session));
 }
 record RuntimeComponents(CliChannel channel){}
}
