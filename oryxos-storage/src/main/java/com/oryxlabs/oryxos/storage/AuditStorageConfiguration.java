package com.oryxlabs.oryxos.storage;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

/** 审计先建表核验后装配；不在模型调用时延迟初始化或自动迁移。 */
@Configuration(proxyBeanMethods = false)
public class AuditStorageConfiguration {
  @Bean(initMethod = "initialize")
  AuditSchemaInitializer auditSchemaInitializer(DataSource source) {
    return new AuditSchemaInitializer(source);
  }

  @Bean
  JpaAuditStore auditStore(
      EntityManagerFactory factory,
      PlatformTransactionManager manager,
      AuditSchemaInitializer initialized) {
    return new JpaAuditStore(factory, manager);
  }
}
