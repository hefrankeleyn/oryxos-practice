package com.oryxlabs.oryxos.core.audit;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
/** 提交未确认的致命故障；不携带数据库异常 cause，防止参数与凭证泄露。 */
public final class AuditPersistenceException extends RuntimeFailure {
    public AuditPersistenceException() { super(Code.AUDIT_UNAVAILABLE); }
}
