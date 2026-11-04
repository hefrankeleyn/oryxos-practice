package com.oryxlabs.oryxos.tool;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.*;
import java.net.URI;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class WhitelistSandboxTest {
 @TempDir Path home;
 @Test void rejectsTraversalSymlinksAndForeignHttpHostsBeforeAnyIo()throws Exception {
  var sandbox=new WhitelistSandbox(List.of(home),Set.of("/bin/echo"),Set.of("allowed.example"));
  sandbox.enforce(SandboxAction.file(SandboxAction.Type.FILE_WRITE,home.resolve("new/child.txt")));
  assertThatThrownBy(()->sandbox.enforce(SandboxAction.file(SandboxAction.Type.FILE_READ,home.resolve("../escape")))).isInstanceOf(RuntimeFailure.class);
  Path outside=Files.createTempDirectory("oryxos-sandbox-");
  try{Files.createSymbolicLink(home.resolve("link"),outside);
   assertThatThrownBy(()->sandbox.enforce(SandboxAction.file(SandboxAction.Type.FILE_WRITE,home.resolve("link/output")))).isInstanceOf(RuntimeFailure.class);
  }finally{Files.deleteIfExists(outside);}
  sandbox.enforce(SandboxAction.http(URI.create("https://allowed.example/path")));
  assertThatThrownBy(()->sandbox.enforce(SandboxAction.http(URI.create("https://allowed.example.evil/path")))).isInstanceOf(RuntimeFailure.class);
  sandbox.enforce(SandboxAction.shell("/bin/echo",List.of("hello")));
  assertThatThrownBy(()->sandbox.enforce(SandboxAction.shell("/bin/bash",List.of("-c","anything")))).isInstanceOf(RuntimeFailure.class);
  assertThat(home.resolve("new")).doesNotExist();
 }
}
