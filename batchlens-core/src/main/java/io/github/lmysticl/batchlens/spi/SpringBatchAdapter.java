package io.github.lmysticl.batchlens.spi;

import io.github.lmysticl.batchlens.catalog.BatchViews;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The Spring Batch calls whose types differ between Spring Batch 5 and 6. Each starter implements
 * this once for its Spring Batch line; validation, locking and the HTTP API live in the core.
 *
 * <p>Implementations translate Spring Batch refusals into {@link
 * io.github.lmysticl.batchlens.BatchLensException}.
 *
 * @param <E> the job execution type of that Spring Batch line
 */
public interface SpringBatchAdapter<E> {
  /** Names of the {@code Job} beans this application can start. */
  Set<String> registeredJobNames();

  /** Job names recorded in the job repository, including jobs that are no longer registered. */
  Collection<String> recordedJobNames();

  /**
   * Executions of the job's newest instances, collected until at least {@code limit} are found or
   * the history ends. The order is unspecified.
   */
  List<E> recentExecutions(String jobName, int limit);

  /** The execution with this ID, or empty when the repository has no such execution. */
  Optional<E> findExecution(long id);

  /** Projects an execution and its steps into the API view. */
  BatchViews.Execution view(E execution);

  /** Launches a registered job with already validated string parameters. */
  E start(String jobName, Map<String, String> parameters);

  /** Launches the execution's job instance again; it resumes from the saved checkpoints. */
  E restart(E execution);

  /** Persists STOPPING so the worker stops at its next chunk boundary. */
  void requestStop(E execution);
}
