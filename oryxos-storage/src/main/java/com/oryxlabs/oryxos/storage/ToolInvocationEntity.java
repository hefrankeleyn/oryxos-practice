package com.oryxlabs.oryxos.storage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/** executed 为 null 表示结果尚未持久确认，不能推断外部操作未发生。 */
@Getter @Setter @Entity @Table(name="tool_invocations")
public class ToolInvocationEntity extends AuditEntity {
    @Column(name="llm_call_id")
    private String llmCallId;
    @Column(name="tool_call_id")
    private String toolCallId;
    @Column(name="tool_name")
    private String toolName;
    @Column(name="executed")
    private Boolean executed;
}
