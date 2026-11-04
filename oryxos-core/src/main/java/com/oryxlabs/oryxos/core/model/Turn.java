package com.oryxlabs.oryxos.core.model;

import java.time.Instant;
import java.util.*;
/** 一次用户输入到最终回复的完整交互；失败原始链保留但不投影到后续模型历史。 */
public final class Turn {
    /** 状态影响历史投影；只有 SUCCEEDED 表示可重放的完整轮。 */
    public enum State { RUNNING, SUCCEEDED, FAILED, LIMIT_REACHED }
    private final UUID id=UUID.randomUUID();
    private final Instant createdAt=Instant.now();
    private Instant endedAt;
    private State state=State.RUNNING;
    private final List<Message> messages=new ArrayList<>();
    public UUID id() { return id; }
    public Instant createdAt() { return createdAt; }
    public Instant endedAt() { return endedAt; }
    public State state() { return state; }
    public List<Message> messages() { return List.copyOf(messages); }
    public void append(Message message) {
        if(state!=State.RUNNING) throw new IllegalStateException("turn closed");
        if(messages.stream().noneMatch(m->m.id().equals(message.id()))) messages.add(message);
    }
    public void finish(State next) {
        if(state!=State.RUNNING || next==State.RUNNING) throw new IllegalStateException("invalid turn transition");
        state=next; endedAt=Instant.now();
    }
}
