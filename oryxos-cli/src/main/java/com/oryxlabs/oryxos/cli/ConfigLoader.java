package com.oryxlabs.oryxos.cli;

import com.oryxlabs.oryxos.core.provider.ProviderDefinition;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.net.URI;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** 外部 YAML 的原始凭证必须为环境引用；在解析值之前校验，禁止把明文密钥当合法配置。 */
public final class ConfigLoader {
  public Map<String, ProviderDefinition> load(Path file, Function<String, String> environment) {
    try (var reader = Files.newBufferedReader(file)) {
      var options = new LoaderOptions();
      options.setAllowDuplicateKeys(false);
      Map<?, ?> root = new Yaml(new SafeConstructor(options)).load(reader);
      Map<?, ?> os = (Map<?, ?>) root.get("oryxos");
      Map<?, ?> providers = (Map<?, ?>) os.get("providers");
      Map<String, ProviderDefinition> result = new LinkedHashMap<>();
      for (var entry : providers.entrySet()) {
        String name = (String) entry.getKey();
        Map<?, ?> config = (Map<?, ?>) entry.getValue();
        String raw = (String) config.get("api-key");
        if (raw == null || !raw.matches("\\$\\{[A-Za-z_][A-Za-z0-9_]*}"))
          throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, "oryxos.providers.api-key");
        String key = environment.apply(raw.substring(2, raw.length() - 1));
        if (key == null || key.isBlank())
          throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, "oryxos.providers.api-key");
        result.put(
            name,
            new ProviderDefinition(
                name,
                URI.create((String) config.get("base-url")),
                key,
                duration(config.get("connect-timeout"), 10),
                duration(config.get("read-timeout"), 60)));
      }
      if (result.isEmpty())
        throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, "oryxos.providers");
      return Map.copyOf(result);
    } catch (RuntimeFailure e) {
      throw e;
    } catch (java.io.IOException | RuntimeException e) {
      throw new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, "runtime.yaml");
    }
  }

  private Duration duration(Object raw, int seconds) {
    if (raw == null) return Duration.ofSeconds(seconds);
    String value = raw.toString();
    if (value.matches("[0-9]+ms"))
      return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
    if (value.matches("[0-9]+s"))
      return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
    return Duration.parse(value);
  }
}
