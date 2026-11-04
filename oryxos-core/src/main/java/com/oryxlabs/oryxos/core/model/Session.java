package com.oryxlabs.oryxos.core.model;

import java.util.*;
/** 仅在进程内存活的会话实例；历史只通过 MemoryService/SessionManager 管理。 */
public final class Session {
    private final UUID id=UUID.randomUUID();
    private final SessionKey key;
    private final List<Turn> turns=new ArrayList<>();
    public Session(SessionKey key) { this.key=key; }
    public UUID id() { return id; }
    public SessionKey key() { return key; }
    public synchronized List<Turn> turns() { return List.copyOf(turns); }
    public synchronized void add(Turn turn) { turns.add(turn); }
}
