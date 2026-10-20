package io.github.lmysticl.batchlens.batch6;

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
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;

/** Spring Batch 6.0: reads from the {@link JobRepository}, launches through the host operator. */
final class SpringBatch6Adapter implements SpringBatchAdapter<JobExecution> {
  private final JobRepository repository;
  private final JobOperator operator;
  private final ObjectProvider<Job> jobs;

  SpringBatch6Adapter(JobRepository repository, JobOperator operator, ObjectProvider<Job> jobs) {
    this.repository = repository;
    this.operator = operator;
    this.jobs = jobs;
  }

  @Override
  public Set<String> registeredJobNames() {
    return jobs.orderedStream().map(Job::getName).collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public Collection<String> recordedJobNames() {
    return repository.getJobNames();
  }

  @Override
  public List<JobExecution> recentExecutions(String jobName, int limit) {
    var executions = new ArrayList<JobExecution>();
    for (var instance : repository.getJobInstances(jobName, 0, limit)) {
      executions.addAll(repository.getJobExecutions(instance));
      if (executions.size() >= limit) break;
    }
    return executions;
  }

  @Override
  public Optional<JobExecution> findExecution(long id) {
    try {
      return Optional.ofNullable(repository.getJobExecution(id));
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
            .map(SpringBatch6Adapter::step)
            .toList();
    var parameters = new LinkedHashMap<String, String>();
    execution
        .getJobParameters()
        .parameters()
        .forEach(parameter -> parameters.put(parameter.name(), String.valueOf(parameter.value())));
    return new BatchViews.Execution(
        String.valueOf(execution.getId()),
        String.valueOf(execution.getJobInstanceId()),
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
      return operator.start(registered(jobName), builder.toJobParameters());
    } catch (JobExecutionException failure) {
      throw BatchLensException.rejectedBySpringBatch(failure);
    }
  }

  @Override
  public JobExecution restart(JobExecution execution) {
    try {
      return operator.restart(execution);
    } catch (JobExecutionException failure) {
      throw BatchLensException.rejectedBySpringBatch(failure);
    }
  }

  @Override
  public void requestStop(JobExecution execution) {
    // Batch 6.0.5 stop(JobExecution) persists detached step contexts and can replace a
    // checkpoint committed by the worker. Signal through the public repository API; the
    // worker owns step completion and checkpoint writes.
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
