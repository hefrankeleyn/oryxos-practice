package com.oryxlabs.oryxos.cli;

import java.nio.file.*;
import java.util.List;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
/** 幂等初始化，只创建缺失项；不跟随越界链接、不覆盖字节、不自动生成 Agent。 */
public final class WorkspaceInitializer {
 public void initialize(Path home){
  try{
   Files.createDirectories(home);Path root=home.toRealPath();
   for(String name:List.of("agents","skills","output","memory","sessions","logs")){
    Path p=root.resolve(name);if(Files.exists(p,LinkOption.NOFOLLOW_LINKS)){if(!Files.isDirectory(p)||!p.toRealPath().startsWith(root))throw new java.io.IOException();}
    else Files.createDirectory(p);
   }
   for(String name:List.of("AGENTS.md","SOUL.md","USER.md")){
    Path p=root.resolve(name);if(Files.exists(p,LinkOption.NOFOLLOW_LINKS)){if(!Files.isRegularFile(p)||!p.toRealPath().startsWith(root))throw new java.io.IOException();}
    else Files.createFile(p);
   }
  }catch(Exception e){throw new RuntimeFailure(RuntimeFailure.Code.CONTEXT_READ,"oryxos.home");}
 }
}
