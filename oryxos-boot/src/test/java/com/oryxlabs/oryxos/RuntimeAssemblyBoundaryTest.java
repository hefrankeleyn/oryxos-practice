package com.oryxlabs.oryxos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.ServerSocket;
import java.util.List;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
class RuntimeAssemblyBoundaryTest {
 @TempDir Path home;
 @Test void defaultToolPoolIsEmptyAndChatCannotBindAWebPort()throws Exception{
  assertThat(new RuntimeConfiguration().toolRegistry(List.of()).names()).isEmpty();
  new com.oryxlabs.oryxos.cli.WorkspaceInitializer().initialize(home);Files.createDirectories(home.resolve("agents/demo"));
  Files.writeString(home.resolve("agents/demo/AGENT.md"),"---\nname: demo\nprovider: {name: fixture, model: model}\n---\nfixture");
  Files.writeString(home.resolve("runtime.yaml"),"oryxos:\n  providers:\n    fixture:\n      base-url: http://127.0.0.1:9\n      api-key: ${FIXTURE_KEY}\n");
  String location=System.getProperty("spring.config.additional-location"),port=System.getProperty("server.port");
  try(var occupied=new ServerSocket(0)){
   System.setProperty("server.port",Integer.toString(occupied.getLocalPort()));System.setProperty("spring.config.additional-location","file:"+home.resolve("runtime.yaml"));
   try(var handle=new BootRuntimeStarter(n->"fixture-key").start(home,"demo")){
    assertThat(handle.interact(new StringReader("\n/quit\n"),new PrintWriter(new StringWriter()),new PrintWriter(new StringWriter()))).isZero();
   }
  }finally{if(location==null)System.clearProperty("spring.config.additional-location");else System.setProperty("spring.config.additional-location",location);if(port==null)System.clearProperty("server.port");else System.setProperty("server.port",port);}
  assertThat(com.oryxlabs.oryxos.core.agent.ProfileContext.get()).isNull();
  assertThat(Path.of("target/classes/com/oryxlabs/oryxos/fixtures/PureCalculationTool.class")).doesNotExist();
 }
}
