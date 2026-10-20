package io.github.lmysticl.batchlens.operations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.BatchLensException.Kind;
import io.github.lmysticl.batchlens.BatchLensProperties;
import io.github.lmysticl.batchlens.support.InMemoryBatch;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class BatchCommandsTest {
  private static final String TOKEN = "a-test-token-with-at-least-32-characters";

  private final InMemoryBatch batch = new InMemoryBatch("invoices");
  private final BatchCommands<InMemoryBatch.Run> commands =
      new BatchCommands<>(batch, new BatchLensProperties(TOKEN, true));

  @Test
  void startsARegisteredJobWithValidatedParameters() {
    var started = commands.start("invoices", Map.of("run", "2026-10"));

    assertThat(started.status()).isEqualTo("COMPLETED");
    assertThat(batch.calls()).containsExactly("start invoices {run=2026-10}");
  }

  @Test
  void rejectsUnregisteredJobsAndInvalidParametersBeforeCallingSpringBatch() {
    assertKind(() -> commands.start("payroll", Map.of()), Kind.NOT_FOUND);
    assertThatThrownBy(() -> commands.start("invoices", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> commands.start("invoices", Map.of("1st", "x")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> commands.start("invoices", Map.of("run", "x".repeat(1025))))
        .isInstanceOf(IllegalArgumentException.class);
    var withNullValue = new HashMap<String, String>();
    withNullValue.put("run", null);
    assertThatThrownBy(() -> commands.start("invoices", withNullValue))
        .isInstanceOf(IllegalArgumentException.class);
    var tooMany = new HashMap<String, String>();
    IntStream.range(0, 21).forEach(index -> tooMany.put("p" + index, "x"));
    assertThatThrownBy(() -> commands.start("invoices", tooMany))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(batch.calls()).isEmpty();
  }

  @Test
  void restartsOnlyFailedOrStoppedExecutionsOfRegisteredJobs() {
    var failed = batch.add("invoices", "FAILED");
    var completed = batch.add("invoices", "COMPLETED");
    var retired = batch.add("legacy", "FAILED");

    assertThat(commands.restart(failed.id()).status()).isEqualTo("COMPLETED");
    assertKind(() -> commands.restart(completed.id()), Kind.CONFLICT);
    assertKind(() -> commands.restart(retired.id()), Kind.CONFLICT);
    assertKind(() -> commands.restart(999), Kind.NOT_FOUND);
    assertThat(batch.calls()).containsExactly("restart " + failed.id());
  }

  @Test
  void stopSignalsARunningExecutionOnce() {
    var running = batch.add("invoices", "STARTED");
    var completed = batch.add("invoices", "COMPLETED");

    assertThat(commands.stop(running.id()).status()).isEqualTo("STOPPING");
    assertThat(commands.stop(running.id()).status()).isEqualTo("STOPPING");
    assertKind(() -> commands.stop(completed.id()), Kind.CONFLICT);
    assertThat(batch.calls()).containsExactly("stop " + running.id());
  }

  @Test
  void everyCommandIsForbiddenWhenOperationsAreDisabled() {
    var readOnly = new BatchCommands<>(batch, new BatchLensProperties(TOKEN, false));
    var failed = batch.add("invoices", "FAILED");

    assertKind(() -> readOnly.start("invoices", Map.of()), Kind.FORBIDDEN);
    assertKind(() -> readOnly.restart(failed.id()), Kind.FORBIDDEN);
    assertKind(() -> readOnly.stop(failed.id()), Kind.FORBIDDEN);
    assertThat(batch.calls()).isEmpty();
  }

  private static void assertKind(ThrowingCallable command, Kind kind) {
    assertThatThrownBy(command)
        .isInstanceOfSatisfying(
            BatchLensException.class, failure -> assertThat(failure.kind()).isEqualTo(kind));
  }
}
