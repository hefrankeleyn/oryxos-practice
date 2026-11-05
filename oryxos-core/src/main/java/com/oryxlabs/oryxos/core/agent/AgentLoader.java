package com.oryxlabs.oryxos.core.agent;

import com.oryxlabs.oryxos.core.model.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import org.yaml.snakeyaml.*;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** 从单个 Agent 目录派生启动快照；正文热读不会重新派生 Profile。 */
public final class AgentLoader {
  private final Set<String> providers, tools;
  private final Function<String, String> environment;

  public AgentLoader(
      Set<String> providers, Set<String> tools, Function<String, String> environment) {
    this.providers = Set.copyOf(providers);
    this.tools = Set.copyOf(tools);
    this.environment = environment;
  }

  public AgentDefinition load(Path workspace, String name) {
    try {
      if (name == null || !name.matches("[\\p{L}\\p{N}_-]+")) throw invalid("agent.name");
      Path root = workspace.toRealPath();
      Path directory = bounded(root, root.resolve("agents").resolve(name));
      var parts = parts(Files.readString(bounded(root, directory.resolve("AGENT.md"))));
      var options = new LoaderOptions();
      options.setAllowDuplicateKeys(false);
      Map<?, ?> front = new Yaml(new SafeConstructor(options)).load(parts[0]);
      var profile = deriveProfile(root, name, front);
      return new AgentDefinition(root, directory, profile);
    } catch (RuntimeFailure e) {
      throw e;
    } catch (java.io.IOException | RuntimeException e) {
      throw invalid("agent");
    }
  }

  public ProfileSnapshot deriveProfile(Path root, String name, Map<?, ?> front)
      throws java.io.IOException {
    if (front == null || !name.equals(front.get("name"))) throw invalid("agent.name");
    for (String unsupported :
        List.of("mcp_servers", "schedule", "schedules", "memory", "notify_channels"))
      if (front.containsKey(unsupported)) throw invalid("agent." + unsupported);
    Map<?, ?> provider = (Map<?, ?>) front.get("provider");
    String p = (String) provider.get("name"), model = resolve((String) provider.get("model"));
    if (!providers.contains(p)) throw invalid("agent.provider.name");
    List<String> allowed = list(front.get("tools"));
    if (!tools.containsAll(allowed)) throw invalid("agent.tools");
    List<Path> bootstrap = new ArrayList<>();
    for (String file : list(front.get("bootstrap"))) {
      Path path = root.resolve(file).normalize();
      bounded(root, path);
      bootstrap.add(path);
    }
    Map<?, ?> settings = front.get("settings") instanceof Map<?, ?> map ? map : Map.of();
    Map<?, ?> identity = front.get("identity") instanceof Map<?, ?> map ? map : Map.of();
    Double temperature =
        provider.get("temperature") == null
            ? null
            : ((Number) provider.get("temperature")).doubleValue();
    return new ProfileSnapshot(
        name,
        p,
        model,
        temperature,
        allowed,
        bootstrap,
        number(settings.get("max_iterations"), 10),
        number(settings.get("max_history_turns"), 20),
        (String) identity.get("prompt"));
  }

  private int number(Object value, int fallback) {
    if (value == null) return fallback;
    if (!(value instanceof Integer i) || i < 1) throw invalid("agent.settings");
    return i;
  }

  private List<String> list(Object value) {
    if (value == null) return List.of();
    if (!(value instanceof List<?> list) || list.stream().anyMatch(x -> !(x instanceof String)))
      throw invalid("agent");
    return list.stream().map(String.class::cast).toList();
  }

  private String resolve(String value) {
    if (value != null && value.matches("\\$\\{[A-Za-z_][A-Za-z0-9_]*}")) {
      String resolved = environment.apply(value.substring(2, value.length() - 1));
      if (resolved == null || resolved.isBlank()) throw invalid("agent.provider.model");
      return resolved;
    }
    if (value == null || value.isBlank() || value.contains("${"))
      throw invalid("agent.provider.model");
    return value;
  }

  static Path bounded(Path root, Path path) throws java.io.IOException {
    Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.startsWith(root) || !normalized.toRealPath().startsWith(root))
      throw new RuntimeFailure(RuntimeFailure.Code.CONTEXT_READ, "agent.context");
    return normalized;
  }

  static String[] parts(String text) {
    String normalized = text.replace("\r\n", "\n");
    if (!normalized.startsWith("---\n")) throw invalid("agent.frontmatter");
    int end = normalized.indexOf("\n---", 4);
    if (end < 0 || (end + 4 < normalized.length() && normalized.charAt(end + 4) != '\n'))
      throw invalid("agent.frontmatter");
    return new String[] {
      normalized.substring(4, end), normalized.substring(Math.min(end + 5, normalized.length()))
    };
  }

  private static RuntimeFailure invalid(String field) {
    return new RuntimeFailure(RuntimeFailure.Code.CONFIGURATION, field);
  }
}
