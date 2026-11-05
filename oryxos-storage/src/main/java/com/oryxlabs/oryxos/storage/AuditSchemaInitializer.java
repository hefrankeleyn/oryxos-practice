package com.oryxlabs.oryxos.storage;

import com.oryxlabs.oryxos.core.audit.AuditPersistenceException;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** 只创建准确的手工 schema；已有表不兼容时停止，不自动迁移或覆盖用户数据。 */
public final class AuditSchemaInitializer {
  private final JdbcTemplate jdbc;

  public AuditSchemaInitializer(DataSource source) {
    jdbc = new JdbcTemplate(source);
  }

  public void initialize() {
    try (var stream = getClass().getResourceAsStream("/db/schema/audit-schema.sql")) {
      String source =
          new String(
              java.util.Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
      for (String ddl : source.split(";")) {
        if (ddl.isBlank()) continue;
        String table = ddl.contains("tool_invocations") ? "tool_invocations" : "llm_calls";
        jdbc.execute(ddl);
        String actual =
            jdbc.queryForObject(
                "select sql from sqlite_master where type='table' and name=?", String.class, table);
        if (actual == null || !normalize(ddl).equals(normalize(actual)))
          throw new AuditPersistenceException();
      }
    } catch (java.io.IOException | RuntimeException failure) {
      throw new AuditPersistenceException();
    }
  }

  private String normalize(String s) {
    return s.replace("IF NOT EXISTS", "").replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
  }
}
