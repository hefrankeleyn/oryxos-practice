package com.oryxlabs.oryxos.cli;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class CliExecutionContractTest {
  @TempDir Path home;

  @Test
  void lightweightInitAndParameterErrorsNeverStartRuntime() {
    String previous = System.getProperty("oryxos.home");
    System.setProperty("oryxos.home", home.toString());
    try {
      assertThat(
              OryxOsCommand.execute(
                  (path, profile) -> {
                    throw new AssertionError("runtime must not start");
                  },
                  "init"))
          .isZero();
      assertThat(OryxOsCommand.execute("chat")).isEqualTo(2);
      assertThat(OryxOsCommand.execute("chat", "--profile", "demo")).isEqualTo(1);
      assertThat(home.resolve("oryxos.db")).doesNotExist();
    } finally {
      if (previous == null) System.clearProperty("oryxos.home");
      else System.setProperty("oryxos.home", previous);
    }
  }
}
