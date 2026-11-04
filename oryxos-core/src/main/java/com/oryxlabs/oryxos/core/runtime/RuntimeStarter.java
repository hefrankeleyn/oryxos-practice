package com.oryxlabs.oryxos.core.runtime;

import java.nio.file.Path;
import java.io.*;
/** Boot 向轻量 CLI 注入的启动端口，不暴露功能模块实现。 */
@FunctionalInterface
public interface RuntimeStarter { RuntimeHandle start(Path home,String profile); }
