package com.oryxlabs.oryxos.memory;
import com.oryxlabs.oryxos.core.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class SessionHistoryProjectionTest {
 @Test void projectsWholeTurnsWithoutDeletingRawHistoryOrMixingSessions(){
  var memory=new DefaultMemoryService(new InMemorySessionManager());var a=memory.session(new SessionKey("cli","a","demo"));var b=memory.session(new SessionKey("cli","b","demo"));
  for(int i=0;i<21;i++){var turn=memory.begin(a,"u"+i);memory.append(turn,new Message(UUID.randomUUID(),turn.id(),1,Message.Role.ASSISTANT,"a"+i,null,List.of(),null,null));memory.finish(turn,Turn.State.SUCCEEDED);}
  var failed=memory.begin(a,"failed");memory.finish(failed,Turn.State.FAILED);var current=memory.begin(a,"current");
  assertThat(memory.project(a,current,20)).hasSize(41).extracting(Message::content).doesNotContain("u0","a0","failed").contains("current");
  assertThat(memory.project(a,current,1)).hasSize(3);assertThat(a.turns()).hasSize(23);assertThat(b.turns()).isEmpty();
  assertThat(memory.session(a.key())).isSameAs(a);
 }
}
