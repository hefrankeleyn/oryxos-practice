package com.oryxlabs.oryxos.tool;

/** 实现无关的调用前白名单检查，不承诺隔离不可信代码。 */
@FunctionalInterface
public interface Sandbox {
  void enforce(SandboxAction action);
}
