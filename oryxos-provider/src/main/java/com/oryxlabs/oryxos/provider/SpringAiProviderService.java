package com.oryxlabs.oryxos.provider;

import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.*;
import java.util.*;
/** 单次同步协议转换；不会执行回调，也不会自行循环。 */
public final class SpringAiProviderService implements ProviderService {
 private final Map<String,ChatModel> models;
 private final List<String> secrets;
 public SpringAiProviderService(Map<String,ChatModel> models){this(models,List.of());}
 public SpringAiProviderService(Map<String,ChatModel> models,Collection<String> secrets){this.models=Map.copyOf(models);this.secrets=secrets.stream().filter(k->k!=null&&!k.isEmpty()).toList();}
 @Override public ProviderResponse call(ProviderRequest request){
  validate(request.profile());
  var model=models.get(request.profile().providerName());
  if(model==null)throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.provider.name");
  try{
   List<org.springframework.ai.chat.messages.Message> messages=new ArrayList<>();messages.add(new SystemMessage(request.system()));
   for(var m:request.messages())switch(m.role()){
    case USER->messages.add(new UserMessage(m.content()));
    case ASSISTANT->messages.add(new AssistantMessage(m.content(),Map.of(),m.toolCalls().stream().map(c->new AssistantMessage.ToolCall(c.id(),"function",c.name(),c.arguments())).toList()));
    case TOOL->messages.add(new ToolResponseMessage(List.of(new ToolResponseMessage.ToolResponse(m.toolCallId(),m.toolName(),m.content()))));
   }
   List<ToolCallback> callbacks=request.tools().stream().map(new ToolSchemaAdapter()::adapt).toList();
   var options=OpenAiChatOptions.builder().model(request.profile().model()).internalToolExecutionEnabled(false).toolCallbacks(callbacks).parallelToolCalls(false);
   if(request.profile().temperature()!=null){double value=request.profile().temperature();if(value<0||value>2)throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.provider.temperature");options.temperature(value);}
   var response=model.call(new Prompt(messages,options.build()));
   if(response==null||response.getResults()==null||response.getResults().size()!=1||response.getResult().getOutput()==null)throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
   var output=response.getResult().getOutput();
   rejectCredentialEcho(output.getText());
   for(var call:output.getToolCalls()){rejectCredentialEcho(call.id());rejectCredentialEcho(call.name());rejectCredentialEcho(call.arguments());}
   var calls=output.getToolCalls().stream().map(c->new ProviderResponse.ToolCall(c.id(),c.name(),c.arguments())).toList();
   Set<String> ids=new HashSet<>();for(var c:calls)if(c.id()==null||c.id().isBlank()||c.name()==null||c.name().isBlank()||!ids.add(c.id()))throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
   if(calls.isEmpty()&&(output.getText()==null||output.getText().isBlank()))throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
   var usage=response.getMetadata().getUsage();boolean known=usage!=null&&usage.getNativeUsage()!=null;
   // DefaultUsage 可能把缺失值归零或求和，审计优先使用供应商原始 nullable 用量字段。
   if(known && usage.getNativeUsage() instanceof org.springframework.ai.openai.api.OpenAiApi.Usage raw)
    return new ProviderResponse(output.getText(),calls,number(raw.promptTokens()),number(raw.completionTokens()),number(raw.totalTokens()));
   return new ProviderResponse(output.getText(),calls,known?number(usage.getPromptTokens()):null,known?number(usage.getCompletionTokens()):null,known?number(usage.getTotalTokens()):null);
  }catch(RuntimeException failure){throw ProviderFailureMapper.map(failure);}
 }
 /** 已选适配器的启动校验；不把其 temperature 范围放入供应商无关的 core 模型。 */
 public void validate(com.oryxlabs.oryxos.core.model.ProfileSnapshot profile){
  if(!models.containsKey(profile.providerName()))throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.provider.name");
  if(profile.temperature()!=null&&(profile.temperature()<0||profile.temperature()>2))throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.provider.temperature");
 }
 private void rejectCredentialEcho(String text){
  if(text!=null&&secrets.stream().anyMatch(text::contains))throw new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
 }
 private Long number(Integer value){return value==null||value<0?null:value.longValue();}
}
