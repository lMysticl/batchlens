package io.github.lmysticl.batchlens.operations;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.BatchLensException.Kind;
import io.github.lmysticl.batchlens.BatchLensProperties;
import io.github.lmysticl.batchlens.catalog.BatchViews;
import io.github.lmysticl.batchlens.spi.SpringBatchAdapter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** Start, restart and stop, guarded by the operations switch and serialized per execution. */
public final class BatchCommands<E> {
  private static final int MAX_PARAMETERS = 20;
  private static final int MAX_VALUE_LENGTH = 1024;
  private static final Pattern PARAMETER_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.-]{0,63}");
  private static final Set<String> RESTARTABLE = Set.of("FAILED", "STOPPED");
  // BatchStatus.isRunning() in Spring Batch 5 and 6.
  private static final Set<String> RUNNING = Set.of("STARTING", "STARTED", "STOPPING");

  private final SpringBatchAdapter<E> batch;
  private final BatchLensProperties properties;
  private final ReentrantLock[] commandLocks = new ReentrantLock[32];

  public BatchCommands(SpringBatchAdapter<E> batch, BatchLensProperties properties) {
    this.batch = batch;
    this.properties = properties;
    for (int index = 0; index < commandLocks.length; index++) {
      commandLocks[index] = new ReentrantLock();
    }
  }

  public BatchViews.Execution start(String jobName, Map<String, String> parameters) {
    requireOperations();
    if (!batch.registeredJobNames().contains(jobName)) {
      throw new BatchLensException(Kind.NOT_FOUND, "Job is not registered in this application");
    }
    validate(parameters);
    return batch.view(batch.start(jobName, parameters));
  }

  public BatchViews.Execution restart(long id) {
    requireOperations();
    return locked(
        id,
        () -> {
          var execution = require(id);
          var view = batch.view(execution);
          if (!RESTARTABLE.contains(view.status())) {
            throw new BatchLensException(
                Kind.CONFLICT, "Only FAILED or STOPPED executions can be restarted");
          }
          if (!batch.registeredJobNames().contains(view.jobName())) {
            throw new BatchLensException(
                Kind.CONFLICT, "The job is no longer registered in this application");
          }
          return batch.view(batch.restart(execution));
        });
  }

  public BatchViews.Execution stop(long id) {
    requireOperations();
    return locked(
        id,
        () -> {
          var execution = require(id);
          var status = batch.view(execution).status();
          if (!RUNNING.contains(status)) {
            throw new BatchLensException(Kind.CONFLICT, "Execution is no longer running");
          }
          if (!"STOPPING".equals(status)) {
            batch.requestStop(execution);
          }
          return batch.view(require(id));
        });
  }

  private E require(long id) {
    return batch.findExecution(id).orElseThrow(BatchLensException::executionNotFound);
  }

  private BatchViews.Execution locked(long id, Supplier<BatchViews.Execution> command) {
    var lock = commandLocks[Math.floorMod(Long.hashCode(id), commandLocks.length)];
    lock.lock();
    try {
      return command.get();
    } finally {
      lock.unlock();
    }
  }

  private void requireOperations() {
    if (!properties.operationsEnabled()) {
      throw new BatchLensException(
          Kind.FORBIDDEN, "Batch operations are disabled by the application owner");
    }
  }

  private static void validate(Map<String, String> parameters) {
    if (parameters == null || parameters.size() > MAX_PARAMETERS) {
      throw new IllegalArgumentException("Supply at most 20 string parameters");
    }
    parameters.forEach(
        (name, value) -> {
          if (!PARAMETER_NAME.matcher(name).matches()
              || value == null
              || value.length() > MAX_VALUE_LENGTH) {
            throw new IllegalArgumentException("Invalid parameter name or value");
          }
        });
  }
}
