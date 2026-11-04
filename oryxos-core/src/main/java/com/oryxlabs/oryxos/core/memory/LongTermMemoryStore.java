package com.oryxlabs.oryxos.core.memory;

/** 长期记忆后端契约；本功能不装配假实现。 */
public interface LongTermMemoryStore { String read(String key); void write(String key,String value); }
