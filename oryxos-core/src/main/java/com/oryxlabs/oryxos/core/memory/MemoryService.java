package com.oryxlabs.oryxos.core.memory;

import com.oryxlabs.oryxos.core.model.*;
import java.util.List;

/** Runtime 唯一记忆入口，模型输入投影不得删除原始历史。 */
public interface MemoryService {
  Session session(SessionKey key);

  Turn begin(Session session, String text);

  List<Message> project(Session session, Turn current, int completedTurns);

  void append(Turn turn, Message message);

  void finish(Turn turn, Turn.State state);
}
