package com.oryxlabs.oryxos.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OryxOsCommand} 与 {@link OryxOsVersionProvider} 的单元测试。
 */
class OryxOsCommandTest {

    /**
     * 版本号应由 Maven 资源过滤注入，不能是占位符或 unknown。
     */
    @Test
    void versionIsInjectedByMaven() {
        String[] lines = new OryxOsVersionProvider().getVersion();

        assertThat(lines[0]).startsWith("OryxOS ").doesNotContain("@").doesNotContain(OryxOsVersionProvider.UNKNOWN);
        assertThat(lines[1]).doesNotContain("@");
    }

    /**
     * {@code --version} 返回 0，并在 stdout 打印版本信息。
     */
    @Test
    void versionOptionPrintsVersion() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new OryxOsCommand());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute("--version");

        assertThat(exitCode).isZero();
        assertThat(out.toString()).startsWith("OryxOS ").contains("Java:");
    }

    /**
     * 不带参数运行：打印版本信息并提示 --help，返回 0。
     */
    @Test
    void noArgsPrintsVersionAndHint() {
        StringWriter out = new StringWriter();
        CommandLine cmd = new CommandLine(new OryxOsCommand());
        cmd.setOut(new PrintWriter(out));

        int exitCode = cmd.execute();

        assertThat(exitCode).isZero();
        assertThat(out.toString()).startsWith("OryxOS ").contains("oryxos --help");
    }
}
