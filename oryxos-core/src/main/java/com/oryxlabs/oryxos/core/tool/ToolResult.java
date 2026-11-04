package com.oryxlabs.oryxos.core.tool;

import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
/** 普通工具失败可回填；审计失败必须抛异常，不能伪装成此结果。 */
public record ToolResult(boolean success,boolean executed,String content,RuntimeFailure.Code error,boolean retryable) {
    @Override public String toString(){return "ToolResult[redacted]";}
}
