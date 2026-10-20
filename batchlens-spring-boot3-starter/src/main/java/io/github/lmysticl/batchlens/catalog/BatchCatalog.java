package io.github.lmysticl.batchlens.catalog;

import io.github.lmysticl.batchlens.BatchLensException;
import io.github.lmysticl.batchlens.BatchLensException.Kind;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.EmptyResultDataAccessException;

public final class BatchCatalog {
  private final JobExplorer explorer;
  private final ObjectProvider<Job> jobs;

  public BatchCatalog(JobExplorer explorer, ObjectProvider<Job> jobs) {
    this.explorer = explorer;
    this.jobs = jobs;
  }

  public List<BatchViews.Job> jobs() {
    var registered = registeredJobs();
    var names = new TreeSet<>(explorer.getJobNames());
    names.addAll(registered.keySet());
    return names.stream()
        .map(name -> new BatchViews.Job(name, registered.containsKey(name)))
        .toList();
  }

  public Map<String, Job> registeredJobs() {
    var result = new LinkedHashMap<String, Job>();
    jobs.orderedStream().forEach(job -> result.put(job.getName(), job));
    return Map.copyOf(result);
  }

  public List<BatchViews.Execution> executions(String jobName, int limit) {
    if (limit < 1 || limit > 50) {
      throw new IllegalArgumentException("History limit must be between 1 and 50");
    }
    var executions = new ArrayList<JobExecution>();
    for (var instance : explorer.getJobInstances(jobName, 0, limit)) {
      executions.addAll(explorer.getJobExecutions(instance));
      if (executions.size() >= limit) break;
    }
    return executions.stream()
        .sorted(Comparator.comparing(JobExecution::getId).reversed())
        .limit(limit)
        .map(this::view)
        .toList();
  }

  public JobExecution requireExecution(long id) {
    try {
      var execution = explorer.getJobExecution(id);
      if (execution != null) return execution;
    } catch (EmptyResultDataAccessException ignored) {
      // JDBC repositories throw for a missing ID; other stores return null.
    }
    throw new BatchLensException(Kind.NOT_FOUND, "Execution does not exist");
  }

  public BatchViews.Execution execution(long id) {
    return view(requireExecution(id));
  }

  public BatchViews.Execution view(JobExecution execution) {
    var steps =
        execution.getStepExecutions().stream()
            .sorted(Comparator.comparing(StepExecution::getId))
            .map(this::step)
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
        instant(execution.getCreateTime()),
        instant(execution.getStartTime()),
        instant(execution.getEndTime()),
        duration(execution.getStartTime(), execution.getEndTime()),
        steps.stream().mapToLong(BatchViews.Step::readCount).sum(),
        steps.stream().mapToLong(BatchViews.Step::writeCount).sum(),
        steps.stream().mapToLong(BatchViews.Step::skipCount).sum(),
        execution.getExitStatus().getExitCode(),
        execution.getExitStatus().getExitDescription(),
        Map.copyOf(parameters),
        steps);
  }

  private BatchViews.Step step(StepExecution step) {
    return new BatchViews.Step(
        String.valueOf(step.getId()),
        step.getStepName(),
        step.getStatus().name(),
        step.getReadCount(),
        step.getWriteCount(),
        step.getSkipCount(),
        step.getCommitCount(),
        step.getRollbackCount(),
        instant(step.getStartTime()),
        instant(step.getEndTime()),
        duration(step.getStartTime(), step.getEndTime()),
        step.getExitStatus().getExitDescription());
  }

  private static String instant(LocalDateTime time) {
    return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toString();
  }

  private static long duration(LocalDateTime start, LocalDateTime end) {
    if (start == null) return 0;
    return Math.max(0, Duration.between(start, end == null ? LocalDateTime.now() : end).toMillis());
  }
}
