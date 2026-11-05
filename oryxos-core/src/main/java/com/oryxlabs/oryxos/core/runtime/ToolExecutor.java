package com.oryxlabs.oryxos.core.runtime;

import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.tool.*;
import java.time.Instant;
import java.util.List;

/** 请求先认领，再提交审计登记、检查/执行、提交终态；任一审计故障都禁止回填继续。 */
public final class ToolExecutor {
  private final ToolInvoker invoker;
  private final AuditStore audit;
  private final ExecutionReceiptStore receipts;

  public ToolExecutor(ToolInvoker invoker, AuditStore audit, ExecutionReceiptStore receipts) {
    this.invoker = invoker;
    this.audit = audit;
    this.receipts = receipts;
  }

  public ToolResult execute(InvocationAudit a, String arguments, List<String> allowed) {
    var key = new ToolRequestIdentity(a.runId(), a.llmCallId(), a.toolCallId());
    var receipt = receipts.claim(key);
    if (receipt.state() == ExecutionReceipt.State.COMPLETED) return receipt.result();
    audit.begin(a);
    long start = System.nanoTime();
    ToolResult result;
    try {
      result = invoker.invoke(a.toolName(), arguments, allowed);
    } catch (AuditPersistenceException fatal) {
      throw fatal;
    } catch (RuntimeException e) {
      result =
          new ToolResult(
              false,
              true,
              SafeErrorMapper.message(RuntimeFailure.Code.TOOL_EXECUTION),
              RuntimeFailure.Code.TOOL_EXECUTION,
              false);
    }
    var status =
        result.success()
            ? InvocationAudit.Status.SUCCEEDED
            : result.executed() ? InvocationAudit.Status.FAILED : InvocationAudit.Status.REJECTED;
    audit.complete(
        a,
        new InvocationAudit.Completion(
            status,
            result.executed(),
            Instant.now(),
            Math.max(0, (System.nanoTime() - start) / 1_000_000),
            result.error(),
            null,
            null,
            null));
    receipts.complete(key, result);
    return result;
  }
}
