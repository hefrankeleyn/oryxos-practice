package com.oryxlabs.oryxos.storage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.oryxlabs.oryxos.core.audit.*;
import java.nio.file.Path;
import java.util.*;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
class AuditFieldConstraintTest {
 @TempDir Path home;
 @Test void unknownTokensDifferFromZeroAndInvalidTerminalCannotPersist(){
  try(var f=new StorageFixture(home)){
   var unknown=StorageFixture.llm();f.audit.begin(unknown);f.audit.complete(unknown,StorageFixture.success(null));
   var zero=StorageFixture.llm();f.audit.begin(zero);f.audit.complete(zero,StorageFixture.success(0L));
   assertThat(f.jdbc.queryForList("select prompt_tokens from llm_calls order by rowid",Integer.class)).containsExactly(null,0);
   for(String set:List.of("status='OTHER'","success=NULL","duration_ms=-1","completed_at=NULL","prompt_tokens=-1","provider=NULL"))
    assertThatThrownBy(()->f.jdbc.update("update llm_calls set "+set+" where id=?",zero.id().toString())).isInstanceOf(RuntimeException.class);
   assertThatThrownBy(()->new InvocationAudit.Completion(InvocationAudit.Status.SUCCEEDED,null,Instant.now(),0,null,-1L,null,null)).isInstanceOf(IllegalArgumentException.class);
   assertThatThrownBy(()->new InvocationAudit(UUID.randomUUID(),InvocationAudit.Kind.LLM,UUID.randomUUID(),"p",UUID.randomUUID(),2,1,Instant.now(),"p","m",null,null,null)).isInstanceOf(IllegalArgumentException.class);
  }
 }
 @Test void toolUniqueKeyTracksIndependentModelResponseAndNullableExecution(){
  try(var f=new StorageFixture(home)){
   var run=UUID.randomUUID();var llm=UUID.randomUUID();
   var first=tool(run,llm);f.audit.begin(first);
   assertThat(f.jdbc.queryForObject("select executed from tool_invocations",Integer.class)).isNull();
   assertThatThrownBy(()->f.audit.begin(tool(run,llm))).isInstanceOf(AuditPersistenceException.class);
   f.audit.begin(tool(run,UUID.randomUUID()));assertThat(f.jdbc.queryForObject("select count(*) from tool_invocations",Integer.class)).isEqualTo(2);
   assertThatThrownBy(()->f.jdbc.update("update tool_invocations set status='REJECTED',success=0,completed_at='now',duration_ms=0,error_code='SANDBOX_DENIED',error_message='denied',executed=NULL where id=?",first.id().toString())).isInstanceOf(RuntimeException.class);
  }
 }
 InvocationAudit tool(UUID run,UUID llm){return new InvocationAudit(UUID.randomUUID(),InvocationAudit.Kind.TOOL,UUID.randomUUID(),"fixture",run,1,10,Instant.now(),null,null,llm,"same-wire","calculate");}
}
