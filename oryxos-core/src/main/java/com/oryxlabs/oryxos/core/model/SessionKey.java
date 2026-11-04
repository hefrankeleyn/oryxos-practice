package com.oryxlabs.oryxos.core.model;

import java.util.Objects;
/** 结构化身份防止分隔符拼接碰撞。 */
public record SessionKey(String channel,String userId,String agentName) {
    public SessionKey { Objects.requireNonNull(channel); Objects.requireNonNull(userId); Objects.requireNonNull(agentName); }
}
