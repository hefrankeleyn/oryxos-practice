package com.oryxlabs.oryxos.core.runtime;

/** 只承载受控类别与字段路径；不保留原始供应商异常 cause。 */
public class RuntimeFailure extends RuntimeException {
    public enum Code { CONFIGURATION, CONTEXT_READ, PROVIDER, PROTOCOL, TOOL_VALIDATION, TOOL_EXECUTION, SANDBOX_DENIED, ITERATION_LIMIT, AUDIT_UNAVAILABLE }
    private final Code code;
    private final String field;
    public RuntimeFailure(Code code) { this(code, ""); }
    public RuntimeFailure(Code code, String field) {
        super(code.name()); this.code=code;
        this.field=field != null && field.matches("[A-Za-z_][A-Za-z0-9_.-]{0,100}") ? field : "";
    }
    public Code code() { return code; }
    public String field() { return field; }
}
