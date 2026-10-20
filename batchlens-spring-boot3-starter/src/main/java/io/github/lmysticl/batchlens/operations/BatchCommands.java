package io.github.lmysticl.batchlens.operations;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.BatchLensException.Kind;
import io.github.lmysticl.batchlens.BatchLensProperties;
import io.github.lmysticl.batchlens.catalog.BatchCatalog;
import io.github.lmysticl.batchlens.catalog.BatchViews;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.dao.OptimisticLockingFailureException;

public final class BatchCommands {
  private final BatchCatalog catalog;
  private final JobLauncher launcher;
  private final JobRepository repository;
  private final BatchLensProperties properties;
  private final ReentrantLock[] commandLocks = new ReentrantLock[32];

  public BatchCommands(
      BatchCatalog catalog,
      JobLauncher launcher,
      JobRepository repository,
      BatchLensProperties properties) {
    this.catalog = catalog;
    this.launcher = launcher;
    this.repository = repository;
    this.properties = properties;
    for (int index = 0; index < commandLocks.length; index++)
      commandLocks[index] = new ReentrantLock();
  }

  public BatchViews.Execution start(String jobName, Map<String, String> parameters) {
    requireOperations();
    var job = catalog.registeredJobs().get(jobName);
    if (job == null)
      throw new BatchLensException(Kind.NOT_FOUND, "Job is not registered in this application");
    var builder = new JobParametersBuilder();
    if (parameters == null || parameters.size() > 20) {
      throw new IllegalArgumentException("Supply at most 20 string parameters");
    }
    parameters.forEach(
        (name, value) -> {
          if (!name.matches("[a-zA-Z][a-zA-Z0-9_.-]{0,63}")
              || value == null
              || value.length() > 1024) {
            throw new IllegalArgumentException("Invalid parameter name or value");
          }
          builder.addString(name, value);
        });
    try {
      return catalog.view(launcher.run(job, builder.toJobParameters()));
    } catch (JobExecutionException failure) {
      throw rejected(failure);
    }
  }

  public BatchViews.Execution restart(long id) {
    requireOperations();
    var lock = lockFor(id);
    lock.lock();
    try {
      var execution = catalog.requireExecution(id);
      if (!Set.of(BatchStatus.FAILED, BatchStatus.STOPPED).contains(execution.getStatus())) {
        throw new BatchLensException(
            Kind.CONFLICT, "Only FAILED or STOPPED executions can be restarted");
      }
      var job = catalog.registeredJobs().get(execution.getJobInstance().getJobName());
      if (job == null) {
        throw new BatchLensException(
            Kind.CONFLICT, "The job is no longer registered in this application");
      }
      // Batch 5 restarts a FAILED or STOPPED instance when it is launched again with the
      // same identifying parameters; the new execution resumes from saved checkpoints.
      return catalog.view(launcher.run(job, execution.getJobParameters()));
    } catch (JobExecutionException failure) {
      throw rejected(failure);
    } finally {
      lock.unlock();
    }
  }

  public BatchViews.Execution stop(long id) {
    requireOperations();
    var lock = lockFor(id);
    lock.lock();
    try {
      var execution = catalog.requireExecution(id);
      if (!execution.isRunning()) {
        throw new BatchLensException(Kind.CONFLICT, "Execution is no longer running");
      }
      if (!execution.isStopping()) {
        // Signal through the public repository API, as the Boot 4 starter does; the
        // worker owns step completion and checkpoint writes.
        execution.setStatus(BatchStatus.STOPPING);
        repository.update(execution);
      }
      return catalog.execution(id);
    } catch (OptimisticLockingFailureException failure) {
      throw new BatchLensException(
          Kind.CONFLICT, "Execution changed while requesting stop. Refresh before retrying.");
    } finally {
      lock.unlock();
    }
  }

  private ReentrantLock lockFor(long id) {
    return commandLocks[Math.floorMod(Long.hashCode(id), commandLocks.length)];
  }

  private void requireOperations() {
    if (!properties.operationsEnabled()) {
      throw new BatchLensException(
          Kind.FORBIDDEN, "Batch operations are disabled by the application owner");
    }
  }

  private static BatchLensException rejected(JobExecutionException failure) {
    return new BatchLensException(
        Kind.CONFLICT,
        "Spring Batch rejected this operation ("
            + failure.getClass().getSimpleName()
            + "). Refresh the execution and check its current state.");
  }
}
