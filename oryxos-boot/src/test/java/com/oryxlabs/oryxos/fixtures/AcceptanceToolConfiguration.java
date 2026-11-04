package com.oryxlabs.oryxos.fixtures;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
/** 必须显式导入的测试配置，绝不打入主 JAR 或自动注册默认 Agent。 */
@TestConfiguration(proxyBeanMethods=false)
public class AcceptanceToolConfiguration {
 @Bean PureCalculationTool calculationFixture(){return new PureCalculationTool();}
 @Bean DeniedActionTool denialFixture(){return new DeniedActionTool();}
}
