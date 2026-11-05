package com.oryxlabs.oryxos.memory;

import com.oryxlabs.oryxos.core.memory.*;
import com.oryxlabs.oryxos.core.model.*;
import java.util.*;

/** 会话访问门面；长期记忆未装配，历史投影不修改原始数据。 */
public final class DefaultMemoryService implements MemoryService {
  private final SessionManager sessions;

  public DefaultMemoryService(SessionManager sessions) {
    this.sessions = sessions;
  }

  public Session session(SessionKey key) {
    return sessions.session(key);
  }

  public Turn begin(Session session, String text) {
    var turn = new Turn();
    turn.append(Message.user(turn, text));
    session.add(turn);
    return turn;
  }

  public List<Message> project(Session session, Turn current, int completedTurns) {
    if (completedTurns < 1) throw new IllegalArgumentException("invalid history limit");
    List<Turn> complete =
        session.turns().stream().filter(t -> t.state() == Turn.State.SUCCEEDED).toList();
    List<Message> result = new ArrayList<>();
    for (Turn t : complete.subList(Math.max(0, complete.size() - completedTurns), complete.size()))
      result.addAll(t.messages());
    result.addAll(current.messages());
    return List.copyOf(result);
  }

  public void append(Turn turn, Message message) {
    turn.append(message);
  }

  public void finish(Turn turn, Turn.State state) {
    turn.finish(state);
  }
}
