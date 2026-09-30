package com.oryxlabs.oryxos;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OryxOS Spring Boot 启动类。
 *
 * <p>位于根包 {@code com.oryxlabs.oryxos}，组件扫描覆盖全部 9 个模块
 * （core / provider / memory / tool / channel.cli / web / storage / cli / boot）。
 *
 * <p>骨架阶段只负责拉起 Spring 上下文；后续由 {@code oryxos-cli} 的 Picocli 入口按子命令决定
 * 是否启动 Spring（{@code chat} / {@code serve} / {@code gateway} 需要，{@code init} 等不需要）。
 */
@Slf4j
@SpringBootApplication
public class OryxOsApplication {

    /**
     * 工作区根目录的系统属性名，默认值为当前目录下的 {@code .oryxos}。
     * 与 {@code application.yaml} 中 {@code oryxos.home} 保持一致。
     */
    static final String HOME_PROPERTY = "oryxos.home";

    /** 工作区默认目录名。 */
    static final String DEFAULT_HOME = ".oryxos";

    /**
     * 进程入口。
     *
     * @param args 命令行参数，原样透传给 Spring Boot
     */
    public static void main(String[] args) {
        Path home = Path.of(System.getProperty(HOME_PROPERTY, DEFAULT_HOME)).toAbsolutePath();
        log.info("OryxOS 启动中，工作区目录: {}", home);
        ensureWorkspace(home);
        SpringApplication.run(OryxOsApplication.class, args);
        log.info("OryxOS 启动完成");
    }

    /**
     * 确保工作区目录存在，SQLite 数据库文件 {@code oryxos.db} 需要落在该目录下。
     *
     * <p>这里只做最小保障（目录存在），完整的工作区初始化（子目录、Bootstrap 模板、幂等）
     * 由 {@code oryxos init} 命令负责。
     *
     * @param home 工作区根目录（绝对路径）
     * @throws UncheckedIOException 目录无法创建时抛出，启动终止
     */
    static void ensureWorkspace(Path home) {
        if (Files.isDirectory(home)) {
            log.debug("工作区目录已存在: {}", home);
            return;
        }
        try {
            Files.createDirectories(home);
            log.warn("工作区目录不存在，已自动创建（建议先执行 oryxos init）: {}", home);
        } catch (IOException e) {
            log.error("无法创建工作区目录: {}", home, e);
            throw new UncheckedIOException("无法创建工作区目录: " + home, e);
        }
    }
}
