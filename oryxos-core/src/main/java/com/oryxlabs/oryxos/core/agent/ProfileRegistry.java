package com.oryxlabs.oryxos.core.agent;

import com.oryxlabs.oryxos.core.model.AgentDefinition;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.util.*;
/** 进程启动派生对象的只读注册表，重复名称拒绝。 */
public final class ProfileRegistry {
 private final Map<String,AgentDefinition> agents;
 public ProfileRegistry(List<AgentDefinition> definitions){Map<String,AgentDefinition> all=new HashMap<>();for(var a:definitions)if(all.putIfAbsent(a.profile().name(),a)!=null)throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.name");agents=Map.copyOf(all);}
 public AgentDefinition get(String name){var agent=agents.get(name);if(agent==null)throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"agent.name");return agent;}
}
