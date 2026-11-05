package com.oryxlabs.oryxos.tool;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.*;
import java.util.*;

/** 应用层白名单；检查规范路径及最近存在父路径，不能作为恶意代码强隔离。 */
public final class WhitelistSandbox implements Sandbox {
  private final List<Path> roots;
  private final Set<String> executables, hosts;

  public WhitelistSandbox(List<Path> roots, Set<String> executables, Set<String> hosts) {
    try {
      this.roots =
          roots.stream()
              .map(
                  p -> {
                    try {
                      return p.toRealPath();
                    } catch (java.io.IOException | RuntimeException e) {
                      throw denied();
                    }
                  })
              .toList();
      this.executables =
          executables.stream()
              .map(
                  e -> {
                    try {
                      return Path.of(e).toRealPath().toString();
                    } catch (java.io.IOException | RuntimeException x) {
                      throw denied();
                    }
                  })
              .collect(java.util.stream.Collectors.toUnmodifiableSet());
      this.hosts =
          hosts.stream()
              .map(h -> h.toLowerCase(Locale.ROOT))
              .collect(java.util.stream.Collectors.toUnmodifiableSet());
    } catch (RuntimeException e) {
      throw denied();
    }
  }

  public void enforce(SandboxAction action) {
    try {
      if (action == null || action.type() == null) throw denied();
      switch (action.type()) {
        case FILE_READ, FILE_WRITE, LIST_DIR -> {
          Path path = Objects.requireNonNull(action.path());
          for (Path segment : path) if (segment.toString().equals("..")) throw denied();
          Path absolute = path.toAbsolutePath().normalize(), existing = absolute;
          while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            if (existing == null) throw denied();
          }
          Path real = existing.toRealPath().resolve(existing.relativize(absolute)).normalize();
          if (roots.stream().noneMatch(real::startsWith)) throw denied();
        }
        case HTTP -> {
          var uri = Objects.requireNonNull(action.uri());
          if (!Set.of("http", "https").contains(uri.getScheme())
              || uri.getHost() == null
              || uri.getUserInfo() != null
              || !hosts.contains(uri.getHost().toLowerCase(Locale.ROOT))) throw denied();
        }
        case SHELL -> {
          String executable = Objects.requireNonNull(action.executable());
          if (!Path.of(executable).isAbsolute()) throw denied();
          String realExecutable = Path.of(executable).toRealPath().toString();
          if (!executables.contains(realExecutable)) throw denied();
          Path fileName = Path.of(realExecutable).getFileName();
          if (fileName == null) throw denied();
          String base = fileName.toString();
          if (Set.of("bash", "sh", "zsh").contains(base) && action.arguments().contains("-c"))
            throw denied();
          for (String arg : action.arguments()) if (arg.indexOf(0) >= 0) throw denied();
        }
      }
    } catch (java.io.IOException | RuntimeException e) {
      throw denied();
    }
  }

  private static RuntimeFailure denied() {
    return new RuntimeFailure(RuntimeFailure.Code.SANDBOX_DENIED);
  }
}
