package io.github.lmysticl.batchlens.catalog;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.spi.SpringBatchAdapter;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** Read-only projections of the job repository. */
public final class BatchCatalog<E> {
  private static final int MAX_HISTORY = 50;
  private static final Comparator<BatchViews.Execution> NEWEST_FIRST =
      Comparator.comparingLong((BatchViews.Execution execution) -> Long.parseLong(execution.id()))
          .reversed();

  private final SpringBatchAdapter<E> batch;

  public BatchCatalog(SpringBatchAdapter<E> batch) {
    this.batch = batch;
  }

  public List<BatchViews.Job> jobs() {
    var registered = batch.registeredJobNames();
    var names = new TreeSet<>(batch.recordedJobNames());
    names.addAll(registered);
    return names.stream().map(name -> new BatchViews.Job(name, registered.contains(name))).toList();
  }

  public List<BatchViews.Execution> executions(String jobName, int limit) {
    if (limit < 1 || limit > MAX_HISTORY) {
      throw new IllegalArgumentException("History limit must be between 1 and " + MAX_HISTORY);
    }
    return batch.recentExecutions(jobName, limit).stream()
        .map(batch::view)
        .sorted(NEWEST_FIRST)
        .limit(limit)
        .toList();
  }

  public BatchViews.Execution execution(long id) {
    return batch.view(batch.findExecution(id).orElseThrow(BatchLensException::executionNotFound));
  }
}
