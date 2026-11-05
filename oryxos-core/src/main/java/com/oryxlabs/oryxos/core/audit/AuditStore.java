package com.oryxlabs.oryxos.core.audit;

/** 同步提交审计；方法正常返回表示事务提交已确认，任何异常均禁止继续调用。 */
public interface AuditStore {
  void begin(InvocationAudit invocation);

  void complete(InvocationAudit invocation, InvocationAudit.Completion completion);
}
