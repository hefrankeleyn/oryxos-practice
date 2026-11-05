package com.oryxlabs.oryxos.tool;

import com.oryxlabs.oryxos.core.runtime.*;
import com.oryxlabs.oryxos.core.tool.*;
import java.util.List;

/** 校验名称/权限/参数后在 IO 前 enforce；只执行一次，不负责审计或模型循环。 */
public final class DefaultToolInvoker implements ToolInvoker {
  private final ToolRegistry registry;
  private final Sandbox sandbox;
  private final ToolInputValidator validator;

  public DefaultToolInvoker(ToolRegistry registry, Sandbox sandbox, ToolInputValidator validator) {
    this.registry = registry;
    this.sandbox = sandbox;
    this.validator = validator;
  }

  public ToolResult invoke(String name, String arguments, List<String> allowed) {
    OryxTool tool = registry.get(name);
    try {
      if (tool == null || !allowed.contains(name))
        throw new RuntimeFailure(RuntimeFailure.Code.TOOL_VALIDATION);
      validator.validate(arguments, tool.inputSchema());
      if (tool instanceof SandboxedTool guarded) sandbox.enforce(guarded.action(arguments));
    } catch (RuntimeFailure failure) {
      if (failure.code() == RuntimeFailure.Code.AUDIT_UNAVAILABLE) throw failure;
      return new ToolResult(
          false, false, SafeErrorMapper.message(failure.code()), failure.code(), false);
    } catch (RuntimeException failure) {
      return new ToolResult(
          false,
          false,
          SafeErrorMapper.message(RuntimeFailure.Code.TOOL_VALIDATION),
          RuntimeFailure.Code.TOOL_VALIDATION,
          false);
    }
    try {
      return new ToolResult(true, true, tool.execute(arguments), null, false);
    } catch (com.oryxlabs.oryxos.core.audit.AuditPersistenceException fatal) {
      throw fatal;
    } catch (Exception failure) {
      return new ToolResult(
          false,
          true,
          SafeErrorMapper.message(RuntimeFailure.Code.TOOL_EXECUTION),
          RuntimeFailure.Code.TOOL_EXECUTION,
          false);
    }
  }
}
