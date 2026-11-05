package com.oryxlabs.oryxos.cli;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Picocli 顶层命令 {@code oryxos}。
 *
 * <p>保留以下基础入口，并通过启动端口提供 init/chat：
 *
 * <ul>
 *   <li>{@code oryxos -V} / {@code oryxos --version}：打印版本信息；
 *   <li>{@code oryxos -h} / {@code oryxos --help}：打印帮助；
 *   <li>{@code oryxos}（不带参数）：打印版本信息并提示查看帮助。
 * </ul>
 *
 * <p>本功能仅增加 init/chat，其余业务命令留给后续功能。
 */
@Slf4j
@Command(
    name = "oryxos",
    mixinStandardHelpOptions = true,
    versionProvider = OryxOsVersionProvider.class,
    description = "OryxOS：Java 原生的企业级 Agent 操作系统（Agent Harness OS）")
public class OryxOsCommand implements Callable<Integer> {

  /** 进程正常结束的返回码。 */
  static final int EXIT_OK = CommandLine.ExitCode.OK;

  /** Picocli 注入的命令模型，用于访问输出流和打印版本 / 帮助。 */
  @Spec CommandSpec spec;

  /**
   * 解析并执行命令行。
   *
   * @param args 命令行参数
   * @return 进程返回码：0 成功，1 执行异常，2 参数错误（Picocli 约定）
   */
  public static int execute(String... args) {
    return execute(null, args);
  }

  /** Boot 显式提供启动端口；命令行参数不写入日志。 */
  public static int execute(
      com.oryxlabs.oryxos.core.runtime.RuntimeStarter starter, String... args) {
    var command =
        new CommandLine(new OryxOsCommand())
            .addSubcommand("init", new InitCommand())
            .addSubcommand("chat", new ChatCommand(starter));
    command.setExecutionExceptionHandler(
        (failure, cmd, parse) -> {
          var safe =
              failure instanceof com.oryxlabs.oryxos.core.runtime.RuntimeFailure f
                  ? f
                  : new com.oryxlabs.oryxos.core.runtime.RuntimeFailure(
                      com.oryxlabs.oryxos.core.runtime.RuntimeFailure.Code.CONFIGURATION,
                      "runtime");
          cmd.getErr().println(com.oryxlabs.oryxos.core.runtime.SafeErrorMapper.render(safe, null));
          cmd.getErr().flush();
          return 1;
        });
    command.setParameterExceptionHandler(
        (failure, arguments) -> {
          failure
              .getCommandLine()
              .getErr()
              .println(
                  com.oryxlabs.oryxos.core.runtime.SafeErrorMapper.render(
                      new com.oryxlabs.oryxos.core.runtime.RuntimeFailure(
                          com.oryxlabs.oryxos.core.runtime.RuntimeFailure.Code.CONFIGURATION,
                          "cli.arguments"),
                      null));
          failure.getCommandLine().getErr().flush();
          return 2;
        });
    return command.execute(args);
  }

  /**
   * 不带任何子命令 / 选项时执行：打印版本信息并提示如何查看帮助。 （{@code --version} 与 {@code --help} 由 Picocli
   * 的标准选项直接处理，不会进入本方法。）
   *
   * @return 返回码 0
   */
  @Override
  @SuppressWarnings("PMD.CloseResource") // PrintWriter 借自 Picocli，不能在命令中关闭宿主输出流。
  public Integer call() {
    log.debug("未指定子命令，打印版本信息");
    CommandLine commandLine = spec.commandLine();
    PrintWriter out = commandLine.getOut();
    commandLine.printVersionHelp(out);
    out.println();
    out.println("使用 `oryxos --help` 查看可用命令。");
    out.flush();
    return EXIT_OK;
  }
}
