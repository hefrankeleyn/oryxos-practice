package com.oryxlabs.oryxos;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
class AuditRetentionIntegrationTest {
 @TempDir Path home;
 @Test void closingAndReopeningKeepsAuditWithoutRestoringConversation()throws Exception{
  UUID previous;
  try(var first=new TestRuntimeHarness(home,10,r->TestRuntimeHarness.done())){first.agent.process(first.session,"original");previous=first.session.id();}
  try(var next=new TestRuntimeHarness(home,10,r->{assertThat(r.messages()).hasSize(1);return TestRuntimeHarness.done();})){
   assertThat(next.session.id()).isNotEqualTo(previous);assertThat(next.session.turns()).isEmpty();
   assertThat(next.storage.jdbc.queryForObject("select count(*) from llm_calls where status='SUCCEEDED'",Integer.class)).isEqualTo(1);
   next.agent.process(next.session,"new");assertThat(next.storage.jdbc.queryForObject("select count(distinct session_id) from llm_calls",Integer.class)).isEqualTo(2);
  }
 }
}
