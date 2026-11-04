package com.oryxlabs.oryxos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import static org.assertj.core.api.Assertions.*;
class CliRuntimeIntegrationTest {
 @TempDir Path home;
 @Test void nonWebRuntimeRetainsConversationAndAuditButNotOldSession()throws Exception {
  var requests=new ArrayList<com.fasterxml.jackson.databind.JsonNode>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/chat/completions",e->{requests.add(new ObjectMapper().readTree(e.getRequestBody()));
   byte[] out="{\"id\":\"r\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"model\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}]}".getBytes();e.getResponseHeaders().add("Content-Type","application/json");e.sendResponseHeaders(200,out.length);e.getResponseBody().write(out);e.close();});server.start();
  String prior=System.getProperty("spring.config.additional-location");
  try{
   new com.oryxlabs.oryxos.cli.WorkspaceInitializer().initialize(home);Files.createDirectories(home.resolve("agents/demo"));
   Files.writeString(home.resolve("runtime.yaml"),"oryxos:\n  providers:\n    fixture:\n      base-url: http://127.0.0.1:"+server.getAddress().getPort()+"\n      api-key: ${FIXTURE_KEY}\n");
   Files.writeString(home.resolve("agents/demo/AGENT.md"),"---\nname: demo\nprovider: {name: fixture, model: model}\nbootstrap: [USER.md]\n---\nbody");
   System.setProperty("spring.config.additional-location","file:"+home.resolve("runtime.yaml"));
   var out=new StringWriter();var err=new StringWriter();var starter=new BootRuntimeStarter(n->"fixture-key");
   try(var handle=starter.start(home,"demo")){assertThat(handle.interact(new StringReader("fact\nrecall\nquestion\n\n/quit\nafter\n"),new PrintWriter(out),new PrintWriter(err))).isZero();}
   assertThat(requests).hasSize(3);assertThat(requests.getLast().get("messages").size()).isEqualTo(6);assertThat(err.toString()).isEmpty();
   try(var handle=starter.start(home,"demo")){handle.interact(new StringReader("new session\n/quit\n"),new PrintWriter(out),new PrintWriter(err));}
   assertThat(requests.getLast().get("messages").size()).isEqualTo(2);
   var jdbc=new org.springframework.jdbc.core.JdbcTemplate(new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:sqlite:"+home.resolve("oryxos.db")));
   assertThat(jdbc.queryForObject("select count(*) from llm_calls",Integer.class)).isEqualTo(4);
   assertThat(jdbc.queryForObject("select count(distinct session_id) from llm_calls",Integer.class)).isEqualTo(2);
  }finally{server.stop(0);if(prior==null)System.clearProperty("spring.config.additional-location");else System.setProperty("spring.config.additional-location",prior);}
 }
}
