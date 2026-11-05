package com.oryxlabs.oryxos.cli;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceInitializerTest {
  @TempDir Path home;

  @Test
  void initializeTwicePreservesBytesAndDoesNotInventAgent() throws Exception {
    var init = new WorkspaceInitializer();
    init.initialize(home);
    for (String name :
        java.util.List.of("agents", "skills", "output", "memory", "sessions", "logs"))
      assertThat(home.resolve(name)).isDirectory();
    for (String name : java.util.List.of("AGENTS.md", "SOUL.md", "USER.md"))
      assertThat(Files.readAllBytes(home.resolve(name))).isEmpty();
    Files.writeString(home.resolve("USER.md"), "existing bytes");
    init.initialize(home);
    assertThat(Files.readString(home.resolve("USER.md"))).isEqualTo("existing bytes");
    try (var files = Files.list(home.resolve("agents"))) {
      assertThat(files.count()).isZero();
    }
  }

  @Test
  void typeConflictDoesNotOverwriteFile() throws Exception {
    Files.writeString(home.resolve("agents"), "preserve");
    assertThatThrownBy(() -> new WorkspaceInitializer().initialize(home))
        .isInstanceOf(com.oryxlabs.oryxos.core.runtime.RuntimeFailure.class);
    assertThat(Files.readString(home.resolve("agents"))).isEqualTo("preserve");
  }
}
