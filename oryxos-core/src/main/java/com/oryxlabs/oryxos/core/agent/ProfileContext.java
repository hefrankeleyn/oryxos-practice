package com.oryxlabs.oryxos.core.agent;

import com.oryxlabs.oryxos.core.model.ProfileSnapshot;

/** 同步调用链中的当前 Profile；必须在最外层 finally 清理，不跨线程传递。 */
public final class ProfileContext {
  private static final ThreadLocal<ProfileSnapshot> CURRENT = new ThreadLocal<>();

  private ProfileContext() {}

  public static void set(ProfileSnapshot profile) {
    CURRENT.set(profile);
  }

  public static ProfileSnapshot get() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
