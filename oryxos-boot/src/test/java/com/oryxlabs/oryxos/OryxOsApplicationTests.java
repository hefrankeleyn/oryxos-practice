package com.oryxlabs.oryxos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 启动冒烟测试：验证 9 个模块聚合后 Spring 上下文能正常加载。
 *
 * <p>使用 SQLite 内存库，避免测试在工作区落盘。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite::memory:")
class OryxOsApplicationTests {

    /**
     * 上下文加载成功即通过。
     */
    @Test
    void contextLoads() {
    }
}
