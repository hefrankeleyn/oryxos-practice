package com.oryxlabs.oryxos.storage;

import com.oryxlabs.oryxos.core.audit.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

final class StorageFixture implements AutoCloseable {
  final JdbcTemplate jdbc;
  final LocalContainerEntityManagerFactoryBean factory;
  final JpaTransactionManager manager;
  final JpaAuditStore audit;

  StorageFixture(Path home) {
    var ds = new DriverManagerDataSource("jdbc:sqlite:" + home.resolve("audit.db"));
    jdbc = new JdbcTemplate(ds);
    new AuditSchemaInitializer(ds).initialize();
    factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(ds);
    factory.setPackagesToScan("com.oryxlabs.oryxos.storage");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.dialect",
            "org.hibernate.community.dialect.SQLiteDialect",
            "hibernate.hbm2ddl.auto",
            "none"));
    factory.afterPropertiesSet();
    manager = new JpaTransactionManager(factory.getObject());
    audit = new JpaAuditStore(factory.getObject(), manager);
  }

  static InvocationAudit llm() {
    return new InvocationAudit(
        UUID.randomUUID(),
        InvocationAudit.Kind.LLM,
        UUID.randomUUID(),
        "fixture",
        UUID.randomUUID(),
        1,
        10,
        Instant.now(),
        "fixture",
        "model",
        null,
        null,
        null);
  }

  static InvocationAudit.Completion success(Long tokens) {
    return new InvocationAudit.Completion(
        InvocationAudit.Status.SUCCEEDED, null, Instant.now(), 1, null, tokens, tokens, tokens);
  }

  public void close() {
    factory.destroy();
  }
}
