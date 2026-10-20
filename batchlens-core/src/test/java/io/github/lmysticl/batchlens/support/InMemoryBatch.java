package io.github.lmysticl.batchlens.support;

import io.github.lmysticl.batchlens.catalog.BatchViews;
import io.github.lmysticl.batchlens.spi.SpringBatchAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** A job repository held in memory; it records the commands the core sends it. */
public final class InMemoryBatch implements SpringBatchAdapter<InMemoryBatch.Run> {
  public record Run(long id, String job, String status) {}

  private final Set<String> registered;
  private final Map<Long, Run> runs = new LinkedHashMap<>();
  private final List<String> calls = new ArrayList<>();
  private long nextId = 1;

  public InMemoryBatch(String... registered) {
    this.registered = Set.of(registered);
  }

  /** Commands received, in order. */
  public List<String> calls() {
    return calls;
  }

  public Run add(String job, String status) {
    var run = new Run(nextId++, job, status);
    runs.put(run.id(), run);
    return run;
  }

  @Override
  public Set<String> registeredJobNames() {
    return registered;
  }

  @Override
  public Collection<String> recordedJobNames() {
    return runs.values().stream().map(Run::job).distinct().toList();
  }

  @Override
  public List<Run> recentExecutions(String jobName, int limit) {
    return runs.values().stream().filter(run -> run.job().equals(jobName)).toList();
  }

  @Override
  public Optional<Run> findExecution(long id) {
    return Optional.ofNullable(runs.get(id));
  }

  @Override
  public BatchViews.Execution view(Run run) {
    return new BatchViews.Execution(
        String.valueOf(run.id()),
        "1",
        run.job(),
        run.status(),
        null,
        null,
        null,
        0,
        0,
        0,
        0,
        "",
        "",
        Map.of(),
        List.of());
  }

  @Override
  public Run start(String jobName, Map<String, String> parameters) {
    calls.add("start " + jobName + " " + parameters);
    return add(jobName, "COMPLETED");
  }

  @Override
  public Run restart(Run run) {
    calls.add("restart " + run.id());
    return add(run.job(), "COMPLETED");
  }

  @Override
  public void requestStop(Run run) {
    calls.add("stop " + run.id());
    runs.put(run.id(), new Run(run.id(), run.job(), "STOPPING"));
  }
}
