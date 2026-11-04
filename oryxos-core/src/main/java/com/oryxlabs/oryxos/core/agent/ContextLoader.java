package com.oryxlabs.oryxos.core.agent;

import com.oryxlabs.oryxos.core.model.AgentDefinition;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.util.*;
/** 每次模型调用前现读正文和已绑定文件；每次使用均重新校验真实路径与软链接目标。 */
public final class ContextLoader {
 public String read(AgentDefinition agent){
  try{
   Path root=agent.workspace().toRealPath();var parts=AgentLoader.parts(Files.readString(AgentLoader.bounded(root,agent.directory().resolve("AGENT.md"))));
   StringBuilder context=new StringBuilder(agent.profile().identityPrompt()).append('\n').append(parts[1]);
   for(Path file:agent.profile().bootstrap())context.append('\n').append(Files.readString(AgentLoader.bounded(root,file)));
   Path skills=agent.directory().resolve("skills");
   if(Files.exists(skills)){
    AgentLoader.bounded(root,skills);
    try(var entries=Files.list(skills)){
     for(Path link:entries.sorted().toList()){
      if(!Files.isSymbolicLink(link)||Files.readSymbolicLink(link).isAbsolute())throw denied();
      Path target=AgentLoader.bounded(root,link).toRealPath();
      if(!target.startsWith(root.resolve("skills").toRealPath()))throw denied();
      Path doc=AgentLoader.bounded(root,target.resolve("SKILL.md"));
      var options=new LoaderOptions();options.setAllowDuplicateKeys(false);
      Map<?,?> metadata=new Yaml(new SafeConstructor(options)).load(AgentLoader.parts(Files.readString(doc))[0]);
      if(!(metadata.get("name") instanceof String name)||!(metadata.get("description") instanceof String description))throw denied();
      context.append('\n').append(name).append('\n').append(description).append('\n').append(doc);
     }
    }
   }
   return context.append('\n').append(ZonedDateTime.now()).toString();
  }catch(Exception e){throw denied();}
 }
 private RuntimeFailure denied(){return new RuntimeFailure(RuntimeFailure.Code.CONTEXT_READ,"agent.context");}
}
