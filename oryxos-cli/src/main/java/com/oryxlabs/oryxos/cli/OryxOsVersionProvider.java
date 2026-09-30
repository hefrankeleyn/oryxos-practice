package com.oryxlabs.oryxos.cli;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.IVersionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * OryxOS 版本信息提供者，供 Picocli 的 {@code --version} 以及无参运行时使用。
 *
 * <p>版本号与构建时间来自 classpath 根目录的 {@code oryxos-version.properties}，
 * 构建时由 Maven 资源过滤注入 {@code project.version} 和 {@code maven.build.timestamp}；
 * 运行环境信息（Java、操作系统）在运行时从系统属性读取。
 */
@Slf4j
public class OryxOsVersionProvider implements IVersionProvider {

    /** 版本信息资源文件（classpath 根目录）。 */
    static final String VERSION_RESOURCE = "/oryxos-version.properties";

    /** 资源缺失或读取失败时的占位值。 */
    static final String UNKNOWN = "unknown";

    /**
     * 生成版本信息，每个元素是输出中的一行。
     *
     * @return 版本信息行，例如 {@code OryxOS 0.1.0-SNAPSHOT}、构建时间、Java 与操作系统信息
     */
    @Override
    public String[] getVersion() {
        Properties props = loadVersionProperties();
        String version = props.getProperty("oryxos.version", UNKNOWN);
        String buildTime = props.getProperty("oryxos.build-time", UNKNOWN);
        log.debug("读取版本信息: version={}, buildTime={}", version, buildTime);

        return new String[]{
                "OryxOS " + version,
                "构建时间: " + buildTime,
                "Java:     " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")",
                "系统:     " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                        + " (" + System.getProperty("os.arch") + ")"
        };
    }

    /**
     * 从 classpath 加载版本资源文件。
     *
     * <p>读取失败不会中断命令，只记录告警日志，并以 {@link #UNKNOWN} 作为版本值。
     *
     * @return 版本属性；资源不存在或读取失败时返回空的 {@link Properties}
     */
    static Properties loadVersionProperties() {
        Properties props = new Properties();
        try (InputStream in = OryxOsVersionProvider.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                log.warn("未找到版本信息资源: {}", VERSION_RESOURCE);
                return props;
            }
            props.load(in);
        } catch (IOException e) {
            log.warn("读取版本信息资源失败: {}", VERSION_RESOURCE, e);
        }
        return props;
    }
}
