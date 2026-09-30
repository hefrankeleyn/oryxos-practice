package com.oryxlabs.oryxos.cli;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

/**
 * Picocli 顶层命令 {@code oryxos}。
 *
 * <p>当前支持：
 * <ul>
 *   <li>{@code oryxos -V} / {@code oryxos --version}：打印版本信息；</li>
 *   <li>{@code oryxos -h} / {@code oryxos --help}：打印帮助；</li>
 *   <li>{@code oryxos}（不带参数）：打印版本信息并提示查看帮助。</li>
 * </ul>
 *
 * <p>后续 12 个子命令（init、status、chat、serve、gateway、profile *、provider list、tool list、
 * session list）将以 {@code subcommands} 形式挂到本命令下。
 */
@Slf4j
@Command(
        name = "oryxos",
        mixinStandardHelpOptions = true,
        versionProvider = OryxOsVersionProvider.class,
        description = "OryxOS：Java 原生的企业级 Agent 操作系统（Agent Harness OS）"
)
public class OryxOsCommand implements Callable<Integer> {

    /** 进程正常结束的返回码。 */
    static final int EXIT_OK = CommandLine.ExitCode.OK;

    /** Picocli 注入的命令模型，用于访问输出流和打印版本 / 帮助。 */
    @Spec
    CommandSpec spec;

    /**
     * 解析并执行命令行。
     *
     * @param args 命令行参数
     * @return 进程返回码：0 成功，1 执行异常，2 参数错误（Picocli 约定）
     */
    public static int execute(String... args) {
        log.debug("执行 oryxos 命令，参数: {}", (Object) args);
        int exitCode = new CommandLine(new OryxOsCommand()).execute(args);
        if (exitCode != EXIT_OK) {
            log.warn("oryxos 命令执行结束，返回码: {}", exitCode);
        } else {
            log.debug("oryxos 命令执行成功");
        }
        return exitCode;
    }

    /**
     * 不带任何子命令 / 选项时执行：打印版本信息并提示如何查看帮助。
     * （{@code --version} 与 {@code --help} 由 Picocli 的标准选项直接处理，不会进入本方法。）
     *
     * @return 返回码 0
     */
    @Override
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
