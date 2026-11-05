package com.oryxlabs.oryxos.storage;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.audit.*;
import com.oryxlabs.oryxos.core.runtime.RuntimeFailure;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

class AuditStoreContractTest {
  @TempDir Path home;
  DriverManagerDataSource source;
  JdbcTemplate jdbc;
  LocalContainerEntityManagerFactoryBean factory;
  JpaAuditStore store;

  @BeforeEach
  void open() {
    source = new DriverManagerDataSource("jdbc:sqlite:" + home.resolve("audit.db"));
    source.setDriverClassName("org.sqlite.JDBC");
    jdbc = new JdbcTemplate(source);
    new AuditSchemaInitializer(source).initialize();
    factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(source);
    factory.setPackagesToScan("com.oryxlabs.oryxos.storage");
    factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
    factory.setJpaPropertyMap(
        Map.of(
            "hibernate.dialect",
            "org.hibernate.community.dialect.SQLiteDialect",
            "hibernate.hbm2ddl.auto",
            "none",
            "hibernate.show_sql",
            "false"));
    factory.afterPropertiesSet();
    store = new JpaAuditStore(factory.getObject(), new JpaTransactionManager(factory.getObject()));
  }

  @AfterEach
  void close() {
    if (factory != null) factory.destroy();
  }

  InvocationAudit invocation(InvocationAudit.Kind kind) {
    return new InvocationAudit(
        UUID.randomUUID(),
        kind,
        UUID.randomUUID(),
        "fixture",
        UUID.randomUUID(),
        1,
        10,
        Instant.now(),
        kind == InvocationAudit.Kind.LLM ? "deepseek" : null,
        kind == InvocationAudit.Kind.LLM ? "fixture-model" : null,
        kind == InvocationAudit.Kind.TOOL ? UUID.randomUUID() : null,
        kind == InvocationAudit.Kind.TOOL ? "call-1" : null,
        kind == InvocationAudit.Kind.TOOL ? "calculate" : null);
  }

  InvocationAudit.Completion completion(
      InvocationAudit.Status status, Boolean executed, RuntimeFailure.Code error) {
    return new InvocationAudit.Completion(
        status, executed, Instant.now(), 2, error, null, null, null);
  }

  @Test
  void beginCommitsBeforeActionAndCompletePersistsWithoutSessionTable() {
    var a = invocation(InvocationAudit.Kind.LLM);
    store.begin(a);
    assertThat(
            jdbc.queryForObject(
                "select status from llm_calls where id=?", String.class, a.id().toString()))
        .isEqualTo("PENDING");
    store.complete(a, completion(InvocationAudit.Status.SUCCEEDED, null, null));
    assertThat(jdbc.queryForMap("select status,success,prompt_tokens from llm_calls"))
        .containsEntry("status", "SUCCEEDED")
        .containsEntry("success", 1)
        .containsEntry("prompt_tokens", null);
    assertThat(jdbc.queryForList("select name from sqlite_master where type='table'", String.class))
        .containsExactlyInAnyOrder("llm_calls", "tool_invocations");
  }

  @Test
  void rejectionIsDurableAndTerminalCannotBeOverwritten() {
    var a = invocation(InvocationAudit.Kind.TOOL);
    store.begin(a);
    store.complete(
        a, completion(InvocationAudit.Status.REJECTED, false, RuntimeFailure.Code.SANDBOX_DENIED));
    assertThat(jdbc.queryForMap("select status,executed,success from tool_invocations"))
        .containsEntry("status", "REJECTED")
        .containsEntry("executed", 0)
        .containsEntry("success", 0);
    assertThatThrownBy(
            () -> store.complete(a, completion(InvocationAudit.Status.SUCCEEDED, true, null)))
        .isInstanceOf(AuditPersistenceException.class);
    assertThat(jdbc.queryForObject("select status from tool_invocations", String.class))
        .isEqualTo("REJECTED");
  }

  @Test
  void schemaIsIdempotentAndIncompatibleSchemaStopsStartup() {
    var a = invocation(InvocationAudit.Kind.LLM);
    store.begin(a);
    new AuditSchemaInitializer(source).initialize();
    assertThat(jdbc.queryForObject("select count(*) from llm_calls", Integer.class)).isEqualTo(1);
    jdbc.execute("alter table llm_calls add column unexpected text");
    assertThatThrownBy(() -> new AuditSchemaInitializer(source).initialize())
        .isInstanceOf(AuditPersistenceException.class);
  }

  @Test
  void databaseFailureIsSafeAndNeverReportedAsCommitted() {
    var a = invocation(InvocationAudit.Kind.LLM);
    jdbc.execute("drop table llm_calls");
    assertThatThrownBy(() -> store.begin(a))
        .isInstanceOf(AuditPersistenceException.class)
        .hasMessage("AUDIT_UNAVAILABLE")
        .hasNoCause();
  }
}
