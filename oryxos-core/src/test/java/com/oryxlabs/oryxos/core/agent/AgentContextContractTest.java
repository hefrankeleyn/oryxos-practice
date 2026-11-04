package com.oryxlabs.oryxos.core.agent;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import static org.assertj.core.api.Assertions.*;
class AgentContextContractTest {
 @TempDir Path home;
 void fixture()throws Exception{
  Files.createDirectories(home.resolve("agents/demo"));Files.createDirectories(home.resolve("skills"));Files.writeString(home.resolve("USER.md"),"old bootstrap");
  Files.writeString(home.resolve("agents/demo/AGENT.md"),"---\nname: demo\nprovider: {name: p, model: m}\nbootstrap: [USER.md]\n---\nold body");
 }
 @Test void configIsFrozenButBodyAndBootstrapReadBeforeEveryCall()throws Exception{
  fixture();var loader=new AgentLoader(Set.of("p"),Set.of(),n->null);var agent=loader.load(home,"demo");
  assertThat(agent.profile().maxIterations()).isEqualTo(10);assertThat(agent.profile().maxHistoryTurns()).isEqualTo(20);
  var context=new ContextLoader();assertThat(context.read(agent)).contains("old body","old bootstrap");
  Files.writeString(home.resolve("agents/demo/AGENT.md"),"---\nname: demo\nprovider: {name: other, model: changed}\nbootstrap: []\n---\nnew body");
  Files.writeString(home.resolve("USER.md"),"new bootstrap");
  assertThat(context.read(agent)).contains("new body","new bootstrap");assertThat(agent.profile().providerName()).isEqualTo("p");
  assertThatThrownBy(()->loader.load(home,"demo")).isInstanceOf(RuntimeFailure.class);
 }
 @Test void traversalAndReplacedBootstrapSymlinkAreRejected()throws Exception{
  fixture();var loader=new AgentLoader(Set.of("p"),Set.of(),n->null);
  assertThatThrownBy(()->loader.load(home,"../demo")).isInstanceOf(RuntimeFailure.class);
  var agent=loader.load(home,"demo");Path outside=Files.createTempFile("oryxos-context-",".md");
  try{Files.delete(home.resolve("USER.md"));Files.createSymbolicLink(home.resolve("USER.md"),outside);
   assertThatThrownBy(()->new ContextLoader().read(agent)).isInstanceOf(RuntimeFailure.class);
  }finally{Files.deleteIfExists(outside);}
 }
 @Test void linkedSkillOnlyInjectsMetadataNotBody()throws Exception{
  fixture();Path source=home.resolve("skills/review");Files.createDirectories(source);Files.writeString(source.resolve("SKILL.md"),"---\nname: review\ndescription: Check inputs\n---\nBODY_MUST_NOT_BE_PRELOADED");
  Files.createDirectories(home.resolve("agents/demo/skills"));Files.createSymbolicLink(home.resolve("agents/demo/skills/review"),Path.of("../../../skills/review"));
  String context=new ContextLoader().read(new AgentLoader(Set.of("p"),Set.of(),n->null).load(home,"demo"));
  assertThat(context).contains("review","Check inputs","SKILL.md").doesNotContain("BODY_MUST_NOT_BE_PRELOADED");
 }
}
