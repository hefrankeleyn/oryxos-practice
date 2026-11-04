package com.oryxlabs.oryxos.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
/** 纯文件初始化，不启动 Spring、数据库或网络客户端。 */
@Command(name="init",mixinStandardHelpOptions=true)
public final class InitCommand implements Callable<Integer>{
 public Integer call(){new WorkspaceInitializer().initialize(Path.of(System.getProperty("oryxos.home",".oryxos")));return 0;}
}
