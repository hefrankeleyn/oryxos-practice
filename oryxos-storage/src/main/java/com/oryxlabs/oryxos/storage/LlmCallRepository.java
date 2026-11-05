package com.oryxlabs.oryxos.storage;

import org.springframework.data.jpa.repository.JpaRepository;

/** 仅供 storage 内部审计持久化查询使用。 */
public interface LlmCallRepository extends JpaRepository<LlmCallEntity, String> {}
