package io.github.lmysticl.batchlens.batch5;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.catalog.BatchViews;
import io.github.lmysticl.batchlens.spi.SpringBatchAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

/** Spring Batch 5.2: reads through the {@link JobExplorer}, launches through the host launcher. */
final class SpringBatch5Adapter implements SpringBatchAdapter<JobExecution> {
  private final JobExplorer explorer;
  private final JobRepository repository;
  private final JobLauncher launcher;
  private final ObjectProvider<Job> jobs;

  SpringBatch5Adapter(
      JobExplorer explorer,
      JobRepository repository,
      JobLauncher launcher,
      ObjectProvider<Job> jobs) {
    this.explorer = explorer;
    this.repository = repository;
    this.launcher = launcher;
    this.jobs = jobs;
  }

  @Override
  public Set<String> registeredJobNames() {
    return jobs.orderedStream().map(Job::getName).collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Collection<String> recordedJobNames() {
    return explorer.getJobNames();
  }

  @Override
  public List<JobExecution> recentExecutions(String jobName, int limit) {
    var executions = new ArrayList<JobExecution>();
    for (var instance : explorer.getJobInstances(jobName, 0, limit)) {
      executions.addAll(explorer.getJobExecutions(instance));
      if (executions.size() >= limit) break;
    }
    return executions;
  }

  @Override
  public Optional<JobExecution> findExecution(long id) {
    try {
      return Optional.ofNullable(explorer.getJobExecution(id));
    } catch (EmptyResultDataAccessException missing) {
      // JDBC repositories throw for a missing ID; other stores return null.
      return Optional.empty();
    }
  }

  @Override
  public BatchViews.Execution view(JobExecution execution) {
    var steps =
        execution.getStepExecutions().stream()
            .sorted(Comparator.comparing(StepExecution::getId))
            .map(SpringBatch5Adapter::step)
            .toList();
    var parameters = new LinkedHashMap<String, String>();
    execution
        .getJobParameters()
        .getParameters()
        .forEach((name, parameter) -> parameters.put(name, String.valueOf(parameter.getValue())));
    return new BatchViews.Execution(
        String.valueOf(execution.getId()),
        String.valueOf(execution.getJobInstance().getInstanceId()),
        execution.getJobInstance().getJobName(),
        execution.getStatus().name(),
        BatchViews.instant(execution.getCreateTime()),
        BatchViews.instant(execution.getStartTime()),
        BatchViews.instant(execution.getEndTime()),
        BatchViews.durationMillis(execution.getStartTime(), execution.getEndTime()),
        BatchViews.total(steps, BatchViews.Step::readCount),
        BatchViews.total(steps, BatchViews.Step::writeCount),
        BatchViews.total(steps, BatchViews.Step::skipCount),
        execution.getExitStatus().getExitCode(),
        execution.getExitStatus().getExitDescription(),
        Map.copyOf(parameters),
        steps);
  }

  @Override
  public JobExecution start(String jobName, Map<String, String> parameters) {
    var builder = new JobParametersBuilder();
    parameters.forEach(builder::addString);
    try {
      return launcher.run(registered(jobName), builder.toJobParameters());
    } catch (JobExecutionException failure) {
      throw BatchLensException.rejectedBySpringBatch(failure);
    }
  }

  @Override
  public JobExecution restart(JobExecution execution) {
    var job = registered(execution.getJobInstance().getJobName());
    try {
      // Batch 5 restarts a FAILED or STOPPED instance when it is launched again with the same
      // identifying parameters; the new execution resumes from the saved checkpoints.
      return launcher.run(job, execution.getJobParameters());
    } catch (JobExecutionException failure) {
      throw BatchLensException.rejectedBySpringBatch(failure);
    }
  }

  @Override
  public void requestStop(JobExecution execution) {
    // Signal through the public repository API, as the Boot 4 starter does; the worker owns
    // step completion and checkpoint writes.
    execution.setStatus(BatchStatus.STOPPING);
    try {
      repository.update(execution);
    } catch (OptimisticLockingFailureException failure) {
      throw BatchLensException.changedDuringStop(failure);
    }
  }

  private Job registered(String jobName) {
    return jobs.orderedStream()
        .filter(job -> job.getName().equals(jobName))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Job " + jobName + " is not registered"));
  }

  private static BatchViews.Step step(StepExecution step) {
    return new BatchViews.Step(
        String.valueOf(step.getId()),
        step.getStepName(),
        step.getStatus().name(),
        step.getReadCount(),
        step.getWriteCount(),
        step.getSkipCount(),
        step.getCommitCount(),
        step.getRollbackCount(),
        BatchViews.instant(step.getStartTime()),
        BatchViews.instant(step.getEndTime()),
        BatchViews.durationMillis(step.getStartTime(), step.getEndTime()),
        step.getExitStatus().getExitDescription());
  }
}
