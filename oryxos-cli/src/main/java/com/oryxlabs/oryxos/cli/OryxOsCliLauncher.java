package com.oryxlabs.oryxos.cli;

/**
 * OryxOS 命令行进程入口（{@code main} 函数所在类）。
 *
 * <p>职责只有两件事：
 * <ol>
 *   <li>在任何 SLF4J Logger 被创建之前，指定 CLI 专用的 logback 配置
 *       （默认只把 WARN 及以上日志写到 stderr，stdout 只留命令输出）；</li>
 *   <li>把参数交给 Picocli 顶层命令 {@link OryxOsCommand} 执行，并以其返回码退出进程。</li>
 * </ol>
 *
 * <p>注意：本类<b>刻意不使用 {@code @Slf4j}</b>。Lombok 生成的静态 Logger 字段会在类初始化时就触发
 * logback 自动配置，早于 {@code main} 中的系统属性设置，导致 CLI 日志配置失效。
 * 真正的业务日志统一在 {@link OryxOsCommand} 及各子命令中输出。
 */
public final class OryxOsCliLauncher {

    /** logback 用于指定配置文件的系统属性名。 */
    static final String LOGBACK_CONFIG_PROPERTY = "logback.configurationFile";

    /** CLI 专用 logback 配置文件（位于 classpath 根目录）。 */
    static final String CLI_LOGBACK_CONFIG = "oryxos-cli-logback.xml";

    /** 工具类，禁止实例化。 */
    private OryxOsCliLauncher() {
    }

    /**
     * 进程入口。
     *
     * @param args 命令行参数，例如 {@code --version}、{@code --help}
     */
    public static void main(String[] args) {
        // 用户未显式指定 logback 配置时，才使用 CLI 默认配置，保留外部覆盖能力
        if (System.getProperty(LOGBACK_CONFIG_PROPERTY) == null) {
            System.setProperty(LOGBACK_CONFIG_PROPERTY, CLI_LOGBACK_CONFIG);
        }
        System.exit(OryxOsCommand.execute(args));
    }
}
