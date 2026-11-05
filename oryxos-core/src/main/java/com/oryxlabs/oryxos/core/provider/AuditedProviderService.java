package com.oryxlabs.oryxos.core.provider;

import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.runtime.*;
import java.time.Instant;

/** 实际调用前确认登记，调用结束后确认终态；complete 故障不可捕获为普通模型失败重做。 */
public final class AuditedProviderService implements ProviderService {
  private final ProviderService delegate;
  private final AuditStore audit;

  public AuditedProviderService(ProviderService delegate, AuditStore audit) {
    this.delegate = delegate;
    this.audit = audit;
  }

  public ProviderResponse call(ProviderRequest r) {
    var a =
        new InvocationAudit(
            r.callId(),
            InvocationAudit.Kind.LLM,
            r.sessionId(),
            r.profile().name(),
            r.runId(),
            r.iteration(),
            r.profile().maxIterations(),
            Instant.now(),
            r.profile().providerName(),
            r.profile().model(),
            null,
            null,
            null);
    audit.begin(a);
    long start = System.nanoTime();
    ProviderResponse response = null;
    RuntimeFailure error = null;
    try {
      response = delegate.call(r);
    } catch (RuntimeException e) {
      error =
          e instanceof RuntimeFailure safe
              ? safe
              : new RuntimeFailure(RuntimeFailure.Code.PROVIDER);
    }
    if (error != null) {
      audit.complete(
          a,
          new InvocationAudit.Completion(
              InvocationAudit.Status.FAILED,
              null,
              Instant.now(),
              elapsed(start),
              error.code(),
              null,
              null,
              null));
      throw error;
    }
    audit.complete(
        a,
        new InvocationAudit.Completion(
            InvocationAudit.Status.SUCCEEDED,
            null,
            Instant.now(),
            elapsed(start),
            null,
            response.promptTokens(),
            response.completionTokens(),
            response.totalTokens()));
    return response;
  }

  private long elapsed(long start) {
    return Math.max(0, (System.nanoTime() - start) / 1_000_000);
  }
}
