package com.oryxlabs.oryxos.memory;

import com.oryxlabs.oryxos.core.memory.SessionManager;
import com.oryxlabs.oryxos.core.model.*;
import java.util.concurrent.ConcurrentHashMap;

/** 进程内身份映射；新的管理器绝不恢复旧会话实例。 */
public final class InMemorySessionManager implements SessionManager {
  private final ConcurrentHashMap<SessionKey, Session> sessions = new ConcurrentHashMap<>();

  public Session session(SessionKey key) {
    return sessions.computeIfAbsent(key, Session::new);
  }
}
