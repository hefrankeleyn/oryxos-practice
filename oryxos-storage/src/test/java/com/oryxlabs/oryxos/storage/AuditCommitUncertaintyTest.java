package com.oryxlabs.oryxos.storage;

import static org.assertj.core.api.Assertions.*;

import com.oryxlabs.oryxos.core.audit.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.*;

class AuditCommitUncertaintyTest {
  @TempDir Path home;

  /** 在真实事务提交前回滚，或真实提交后丢失确认；不靠内存审计替身模拟持久性。 */
  PlatformTransactionManager fault(StorageFixture fixture, int failAt, boolean after) {
    return new PlatformTransactionManager() {
      int commits;

      public TransactionStatus getTransaction(TransactionDefinition d) {
        return fixture.manager.getTransaction(d);
      }

      public void rollback(TransactionStatus s) {
        fixture.manager.rollback(s);
      }

      public void commit(TransactionStatus s) {
        if (++commits == failAt) {
          if (after) fixture.manager.commit(s);
          else fixture.manager.rollback(s);
          throw new TransactionSystemException("fixture commit acknowledgement lost");
        }
        fixture.manager.commit(s);
      }
    };
  }

  @Test
  void uncertainBeginNeverStartsActionEvenIfPendingWasActuallyCommitted() {
    try (var f = new StorageFixture(home)) {
      var store = new JpaAuditStore(f.factory.getObject(), fault(f, 1, true));
      var count = new AtomicInteger();
      assertThatThrownBy(
              () -> {
                store.begin(StorageFixture.llm());
                count.incrementAndGet();
              })
          .isInstanceOf(AuditPersistenceException.class);
      assertThat(count.get()).isZero();
      assertThat(f.jdbc.queryForObject("select status from llm_calls", String.class))
          .isEqualTo("PENDING");
    }
  }

  @Test
  void preCommitFailureKeepsPendingAndPostCommitUncertaintyKeepsTerminal() {
    for (boolean after : new boolean[] {false, true}) {
      Path directory = home.resolve(Boolean.toString(after));
      try {
        java.nio.file.Files.createDirectories(directory);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      try (var f = new StorageFixture(directory)) {
        var store = new JpaAuditStore(f.factory.getObject(), fault(f, 2, after));
        var a = StorageFixture.llm();
        var count = new AtomicInteger();
        store.begin(a);
        count.incrementAndGet();
        assertThatThrownBy(() -> store.complete(a, StorageFixture.success(null)))
            .isInstanceOf(AuditPersistenceException.class);
        assertThat(f.jdbc.queryForObject("select status from llm_calls", String.class))
            .isEqualTo(after ? "SUCCEEDED" : "PENDING");
        assertThat(count.get()).isEqualTo(1);
      }
    }
  }
}
