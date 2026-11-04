package com.oryxlabs.oryxos.core.memory;

import com.oryxlabs.oryxos.core.model.*;
/** 进程内按结构化身份获取会话，不承诺跨重启恢复。 */
public interface SessionManager { Session session(SessionKey key); }
