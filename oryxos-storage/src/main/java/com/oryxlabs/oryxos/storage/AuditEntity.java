package com.oryxlabs.oryxos.storage;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 两类审计共享安全快照；无 Session 实体关系，也不保存任意输入输出正文。 */
@Getter
@Setter
@MappedSuperclass
public abstract class AuditEntity {
  @Id private String id;

  @Column(name = "session_id")
  private String sessionId;

  @Column(name = "profile_name")
  private String profileName;

  @Column(name = "run_id")
  private String runId;

  @Column(name = "iteration")
  private Integer iteration;

  @Column(name = "status")
  private String status;

  @Column(name = "success")
  private Boolean success;

  @Column(name = "started_at")
  private String startedAt;

  @Column(name = "completed_at")
  private String completedAt;

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "error_code")
  private String errorCode;

  @Column(name = "error_message")
  private String errorMessage;
}
