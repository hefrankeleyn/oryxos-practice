package com.oryxlabs.oryxos.cli;

import java.nio.file.Path;
import java.io.*;
import java.util.concurrent.Callable;
import com.oryxlabs.oryxos.core.runtime.*;
import picocli.CommandLine.*;
/** 只依赖启动端口；独立 exec 缺 Runtime 时明确失败，不临时拼接另一套运行时。 */
@Command(name="chat",mixinStandardHelpOptions=true)
public final class ChatCommand implements Callable<Integer>{
 private final RuntimeStarter starter;
 @Option(names="--profile",required=true) String profile;
 @Spec picocli.CommandLine.Model.CommandSpec spec;
 public ChatCommand(RuntimeStarter starter){this.starter=starter;}
 public Integer call(){
  if(starter==null)throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION,"runtime");
  try(var handle=starter.start(Path.of(System.getProperty("oryxos.home",".oryxos")).toAbsolutePath(),profile)){
   return handle.interact(new InputStreamReader(System.in,java.nio.charset.StandardCharsets.UTF_8),spec.commandLine().getOut(),spec.commandLine().getErr());
  }
 }
}
