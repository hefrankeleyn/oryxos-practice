package com.oryxlabs.oryxos.core.audit;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.time.Instant;
import java.util.UUID;

/** 只含安全关联字段的调用登记；不接收 Prompt、参数、响应或原始异常对象。 */
public record InvocationAudit(
    UUID id,
    Kind kind,
    UUID sessionId,
    String profileName,
    UUID runId,
    int iteration,
    int maxIterations,
    Instant startedAt,
    String provider,
    String model,
    UUID llmCallId,
    String toolCallId,
    String toolName) {
  public enum Kind {
    LLM,
    TOOL
  }

  /** 持久化状态：PENDING 不能证明外部动作未发生；终态不可倒退。 */
  public enum Status {
    PENDING,
    SUCCEEDED,
    FAILED,
    REJECTED
  }

  public InvocationAudit {
    if (id == null
        || kind == null
        || sessionId == null
        || profileName == null
        || profileName.isBlank()
        || runId == null
        || startedAt == null
        || iteration < 1
        || iteration > maxIterations) throw new IllegalArgumentException("invalid audit identity");
    if (kind == Kind.LLM
        && (provider == null || provider.isBlank() || model == null || model.isBlank()))
      throw new IllegalArgumentException("invalid model target");
    if (kind == Kind.TOOL
        && (llmCallId == null
            || toolCallId == null
            || toolCallId.isBlank()
            || toolName == null
            || toolName.isBlank())) throw new IllegalArgumentException("invalid tool identity");
  }

  /** 终态时间与耗时使用不同来源；耗时为单调时钟测量，缺失 token 保持 null。 */
  public record Completion(
      Status status,
      Boolean executed,
      Instant completedAt,
      long durationMs,
      RuntimeFailure.Code error,
      Long promptTokens,
      Long completionTokens,
      Long totalTokens) {
    public Completion {
      if (status == null
          || status == Status.PENDING
          || completedAt == null
          || durationMs < 0
          || (status == Status.SUCCEEDED) != (error == null)
          || negative(promptTokens)
          || negative(completionTokens)
          || negative(totalTokens)) throw new IllegalArgumentException("invalid audit completion");
    }

    private static boolean negative(Long v) {
      return v != null && v < 0;
    }
  }
}
