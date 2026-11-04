package com.oryxlabs.oryxos.core.runtime;

import com.oryxlabs.oryxos.core.tool.ToolResult;
/** 进程内执行收据；只有审计终态提交后才允许进入 COMPLETED。 */
public final class ExecutionReceipt {
 public enum State { RECEIVED, CLAIMED, COMPLETED }
 private State state=State.RECEIVED;private ToolResult result;
 public State state(){return state;}
 public ToolResult result(){return result;}
 void claim(){state=State.CLAIMED;}
 void complete(ToolResult result){this.result=result;state=State.COMPLETED;}
}
