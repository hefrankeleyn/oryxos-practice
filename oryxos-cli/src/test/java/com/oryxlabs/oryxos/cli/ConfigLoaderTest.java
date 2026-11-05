package com.oryxlabs.oryxos.cli;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {
  @TempDir Path home;

  @Test
  void requiresOriginalEnvironmentPlaceholderAndRedactsSecrets() throws Exception {
    Path config = home.resolve("runtime.yaml");
    Files.writeString(
        config,
        "oryxos:\n  providers:\n    deepseek:\n      base-url: https://api.deepseek.com\n      api-key: ${FIXTURE_KEY}\n");
    assertThatThrownBy(() -> new ConfigLoader().load(config, n -> null))
        .isInstanceOf(RuntimeFailure.class)
        .hasNoCause();
    var value = new ConfigLoader().load(config, n -> "fixture-secret").get("deepseek");
    assertThat(value.toString()).doesNotContain("fixture-secret");
    assertThat(value.readTimeout()).isEqualTo(java.time.Duration.ofSeconds(60));
    Files.writeString(config, Files.readString(config).replace("${FIXTURE_KEY}", "fixture-secret"));
    assertThatThrownBy(() -> new ConfigLoader().load(config, n -> "anything"))
        .isInstanceOf(RuntimeFailure.class)
        .hasNoCause();
  }
}
