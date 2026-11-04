package com.oryxlabs.oryxos.storage;
import com.oryxlabs.oryxos.core.audit.AuditPersistenceException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;
class AuditSchemaInitializationTest {
 @TempDir Path home;
 @Test void freshIdempotentSchemaAndRequiredConstraints() {
   var ds=new DriverManagerDataSource("jdbc:sqlite:"+home.resolve("audit.db"));
   new AuditSchemaInitializer(ds).initialize();new AuditSchemaInitializer(ds).initialize();
   var jdbc=new JdbcTemplate(ds);
   assertThat(jdbc.queryForList("select name from sqlite_master where type='table'",String.class)).containsExactlyInAnyOrder("llm_calls","tool_invocations");
   assertThatThrownBy(()->jdbc.execute("insert into llm_calls(id) values('bad')")).isInstanceOf(RuntimeException.class);
 }
 @Test void inaccessibleDatabaseDoesNotClaimSuccess() {
   var ds=new DriverManagerDataSource("jdbc:sqlite:"+home.resolve("missing/child/audit.db"));
   assertThatThrownBy(()->new AuditSchemaInitializer(ds).initialize()).isInstanceOf(AuditPersistenceException.class).hasNoCause();
 }
}
