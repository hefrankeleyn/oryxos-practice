package com.oryxlabs.oryxos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.provider.*;
import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.provider.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.openai.OpenAiChatOptions;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;
class AuditedProviderIntegrationTest {
 @TempDir Path home;
 static ProviderRequest request(String name){return new ProviderRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,new ProfileSnapshot("fixture",name,"model",null,List.of(),List.of(),10,20,""),"",List.of(),List.of());}
 @Test void independentMinimumAssemblyCommitsSuccessFailureAndRoutesOnlyChosenModel(){
  try(var runtime=new TestAuditRuntime(home)){
   var calls=new AtomicInteger();var other=new AtomicInteger();
   ChatModel first=p->{calls.incrementAndGet();assertThat(((OpenAiChatOptions)p.getOptions()).getInternalToolExecutionEnabled()).isFalse();return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));};
   ChatModel second=p->{other.incrementAndGet();throw new IllegalStateException("fixture-secret");};
   var service=new AuditedProviderService(new SpringAiProviderService(Map.of("a",first,"b",second)),runtime.audit);
   service.call(request("a"));assertThat(other.get()).isZero();assertThat(calls.get()).isEqualTo(1);
   assertThatThrownBy(()->service.call(request("b"))).hasMessage("PROVIDER").hasNoCause();
   assertThat(runtime.jdbc.queryForList("select status from llm_calls order by rowid",String.class)).containsExactly("SUCCEEDED","FAILED");
   assertThat(runtime.jdbc.queryForList("select error_message from llm_calls where status='FAILED'",String.class).toString()).doesNotContain("fixture-secret");
  }
 }
 @Test void beginFailureMakesZeroCallsAndCompleteFailureCannotStartAnotherCall(){
  try(var runtime=new TestAuditRuntime(home)){
   var count=new AtomicInteger();ProviderService model=r->{count.incrementAndGet();return new ProviderResponse("ok",List.of(),null,null,null);};
   AuditStore before=new AuditStore(){public void begin(InvocationAudit a){throw new AuditPersistenceException();}public void complete(InvocationAudit a,InvocationAudit.Completion c){fail("complete unreachable");}};
   assertThatThrownBy(()->new AuditedProviderService(model,before).call(request("a"))).isInstanceOf(AuditPersistenceException.class);assertThat(count.get()).isZero();
   AuditStore after=new AuditStore(){public void begin(InvocationAudit a){runtime.audit.begin(a);}public void complete(InvocationAudit a,InvocationAudit.Completion c){throw new AuditPersistenceException();}};
   assertThatThrownBy(()->{var service=new AuditedProviderService(model,after);service.call(request("a"));service.call(request("a"));}).isInstanceOf(AuditPersistenceException.class);
   assertThat(count.get()).isEqualTo(1);assertThat(runtime.jdbc.queryForObject("select status from llm_calls",String.class)).isEqualTo("PENDING");
  }
 }
}
