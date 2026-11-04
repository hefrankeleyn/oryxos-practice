package com.oryxlabs.oryxos;
import com.oryxlabs.oryxos.storage.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import java.nio.file.Path;
import java.util.Map;
/** 仅测试的最小持久化装配，无应用扫描、CLI、ReAct 或 Web 上下文。 */
final class TestAuditRuntime implements AutoCloseable {
 final JdbcTemplate jdbc;
 final JpaAuditStore audit;
 final LocalContainerEntityManagerFactoryBean factory;
 TestAuditRuntime(Path home){
  var ds=new DriverManagerDataSource("jdbc:sqlite:"+home.resolve("oryxos.db"));jdbc=new JdbcTemplate(ds);
  new AuditSchemaInitializer(ds).initialize();factory=new LocalContainerEntityManagerFactoryBean();factory.setDataSource(ds);
  factory.setPackagesToScan("com.oryxlabs.oryxos.storage");factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
  factory.setJpaPropertyMap(Map.of("hibernate.dialect","org.hibernate.community.dialect.SQLiteDialect","hibernate.hbm2ddl.auto","none"));factory.afterPropertiesSet();
  audit=new JpaAuditStore(factory.getObject(),new JpaTransactionManager(factory.getObject()));
 }
 @Override public void close(){factory.destroy();}
}
