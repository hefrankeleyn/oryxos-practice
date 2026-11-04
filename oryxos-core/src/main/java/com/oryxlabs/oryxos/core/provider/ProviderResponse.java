package com.oryxlabs.oryxos.core.provider;

import java.util.List;
/** 供应商无关单次响应；用量缺失为 null，不以零冒充。 */
public record ProviderResponse(String text,List<ToolCall> toolCalls,Long promptTokens,Long completionTokens,Long totalTokens) {
    public ProviderResponse { text=text==null?"":text; toolCalls=List.copyOf(toolCalls); }
    public record ToolCall(String id,String name,String arguments) { @Override public String toString(){return "ToolCall[redacted]";} }
    @Override public String toString(){return "ProviderResponse[redacted]";}
}
