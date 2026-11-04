package com.oryxlabs.oryxos.core.agent;

import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.memory.MemoryService;
import com.oryxlabs.oryxos.core.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/** 所有触发来源的统一入口；同会话串行，不把远程 IO 包入数据库事务。 */
public final class AgentService {
 private static final Logger log=LoggerFactory.getLogger(AgentService.class);
 private final ProfileRegistry profiles;private final MemoryService memory;private final ReActLoop loop;
 public AgentService(ProfileRegistry profiles,MemoryService memory,ReActLoop loop){this.profiles=profiles;this.memory=memory;this.loop=loop;}
 public String process(Session session,String input){
  if(input==null||input.isBlank())return "";
  synchronized(session){
   var agent=profiles.get(session.key().agentName());ProfileContext.set(agent.profile());Turn turn=null;
   try{turn=memory.begin(session,input);log.debug("处理交互轮 {}",turn.id());return loop.process(session,turn,agent);}
   catch(RuntimeException error){
    var safe=error instanceof RuntimeFailure r?r:new RuntimeFailure(RuntimeFailure.Code.PROTOCOL);
    if(turn!=null&&turn.state()==Turn.State.RUNNING)memory.finish(turn,safe.code()==RuntimeFailure.Code.ITERATION_LIMIT?Turn.State.LIMIT_REACHED:Turn.State.FAILED);
    log.warn("处理失败，类别 {}，交互轮 {}",safe.code(),turn==null?null:turn.id());throw safe;
   }finally{ProfileContext.clear();}
  }
 }
}
