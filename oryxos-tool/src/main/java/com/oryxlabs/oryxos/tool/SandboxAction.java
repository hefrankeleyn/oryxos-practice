package com.oryxlabs.oryxos.tool;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

/** 真实操作的结构化声明；Shell 使用可执行文件及参数数组，不接受拼接命令。 */
public record SandboxAction(
    Type type, Path path, String executable, List<String> arguments, URI uri) {
  public enum Type {
    FILE_READ,
    FILE_WRITE,
    LIST_DIR,
    SHELL,
    HTTP
  }

  public SandboxAction {
    arguments = arguments == null ? List.of() : List.copyOf(arguments);
  }

  public static SandboxAction file(Type type, Path path) {
    return new SandboxAction(type, path, null, List.of(), null);
  }

  public static SandboxAction shell(String executable, List<String> arguments) {
    return new SandboxAction(Type.SHELL, null, executable, arguments, null);
  }

  public static SandboxAction http(URI uri) {
    return new SandboxAction(Type.HTTP, null, null, List.of(), uri);
  }
}
