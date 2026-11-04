package com.oryxlabs.oryxos.storage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
/** 模型尝试审计；token 未知保持 null。 */
@Getter @Setter @Entity @Table(name="llm_calls")
public class LlmCallEntity extends AuditEntity {
    @Column(name="provider")
    private String provider;
    @Column(name="model")
    private String model;
    @Column(name="prompt_tokens")
    private Long promptTokens;
    @Column(name="completion_tokens")
    private Long completionTokens;
    @Column(name="total_tokens")
    private Long totalTokens;
}
