package com.oryxlabs.oryxos.fixtures;
import com.oryxlabs.oryxos.core.tool.OryxTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicInteger;
/** 仅验收源码中的纯计算工具，无文件、网络或通知副作用。 */
public final class PureCalculationTool implements OryxTool {
 public final AtomicInteger executions=new AtomicInteger();
 public String name(){return "calculate";}
 public String description(){return "Add two integers";}
 public String inputSchema(){return "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"integer\"},\"b\":{\"type\":\"integer\"}},\"required\":[\"a\",\"b\"],\"additionalProperties\":false}";}
 public String execute(String arguments)throws Exception {executions.incrementAndGet();var n=new ObjectMapper().readTree(arguments);return n.get("a").bigIntegerValue().add(n.get("b").bigIntegerValue()).toString();}
}
