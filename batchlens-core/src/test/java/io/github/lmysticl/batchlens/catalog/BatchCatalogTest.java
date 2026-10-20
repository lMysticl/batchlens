package io.github.lmysticl.batchlens.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.support.InMemoryBatch;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BatchCatalogTest {
  private final InMemoryBatch batch = new InMemoryBatch("invoices", "payroll");
  private final BatchCatalog<InMemoryBatch.Run> catalog = new BatchCatalog<>(batch);

  @Test
  void listsRegisteredAndRecordedJobsByName() {
    batch.add("legacy", "COMPLETED");
    batch.add("invoices", "FAILED");

    assertThat(catalog.jobs())
        .containsExactly(
            new BatchViews.Job("invoices", true),
            new BatchViews.Job("legacy", false),
            new BatchViews.Job("payroll", true));
  }

  @Test
  void returnsTheNewestExecutionsFirstWithinTheLimit() {
    for (int run = 0; run < 12; run++) batch.add("invoices", "COMPLETED");

    assertThat(catalog.executions("invoices", 3))
        .extracting(BatchViews.Execution::id)
        .containsExactly("12", "11", "10");
    assertThatThrownBy(() -> catalog.executions("invoices", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> catalog.executions("invoices", 51))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportsAMissingExecution() {
    assertThatThrownBy(() -> catalog.execution(42))
        .isInstanceOfSatisfying(
            BatchLensException.class,
            failure -> assertThat(failure.kind()).isEqualTo(BatchLensException.Kind.NOT_FOUND));
  }

  @Test
  void rendersTimesAsInstantsAndDurationsFromTheStart() {
    var start = LocalDateTime.of(2026, 10, 20, 9, 0);

    assertThat(BatchViews.instant(null)).isNull();
    assertThat(BatchViews.instant(start)).endsWith("Z");
    assertThat(BatchViews.durationMillis(null, start)).isZero();
    assertThat(BatchViews.durationMillis(start, start.plusSeconds(90))).isEqualTo(90_000);
    assertThat(BatchViews.durationMillis(start.plusSeconds(5), start)).isZero();
  }
}
