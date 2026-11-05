package com.oryxlabs.oryxos.core.runtime;

import java.io.*;

/** 启动句柄只暴露交互与关闭，CLI 不依赖 Boot 或功能实现。 */
public interface RuntimeHandle extends AutoCloseable {
  int interact(Reader input, PrintWriter out, PrintWriter error);

  @Override
  void close();
}
