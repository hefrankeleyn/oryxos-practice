package com.oryxlabs.oryxos.fixtures;
import com.oryxlabs.oryxos.tool.*;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
/** 使用真实动作类型验证拒绝先于执行；execute 仅计数，不发送网络请求。 */
public final class DeniedActionTool implements SandboxedTool {
 public final AtomicInteger executions=new AtomicInteger();
 public String name(){return "denied_action";}
 public String description(){return "Denied action fixture";}
 public String inputSchema(){return "{\"type\":\"object\",\"properties\":{},\"additionalProperties\":false}";}
 public SandboxAction action(String ignored){return SandboxAction.http(URI.create("https://denied.example/"));}
 public String execute(String ignored){executions.incrementAndGet();return "unexpected";}
}
