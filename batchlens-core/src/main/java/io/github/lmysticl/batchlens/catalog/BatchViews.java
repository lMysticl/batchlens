package io.github.lmysticl.batchlens.catalog;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

/** The JSON shapes of the BatchLens API and how Spring Batch values are rendered into them. */
public final class BatchViews {
  private BatchViews() {}

  public record Info(String name, String version, boolean operationsEnabled, boolean demo) {}

  public record Job(String name, boolean registered) {}

  public record Step(
      String id,
      String name,
      String status,
      long readCount,
      long writeCount,
      long skipCount,
      long commitCount,
      long rollbackCount,
      String startTime,
      String endTime,
      long durationMillis,
      String error) {}

  public record Execution(
      String id,
      String instanceId,
      String jobName,
      String status,
      String createTime,
      String startTime,
      String endTime,
      long durationMillis,
      long readCount,
      long writeCount,
      long skipCount,
      String exitCode,
      String error,
      Map<String, String> parameters,
      List<Step> steps) {}

  /** Spring Batch timestamps are local to the JVM time zone; the API uses ISO-8601 instants. */
  public static String instant(LocalDateTime time) {
    return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toString();
  }

  /** Milliseconds from start to end, or to now while still running; 0 before the start. */
  public static long durationMillis(LocalDateTime start, LocalDateTime end) {
    if (start == null) return 0;
    return Math.max(0, Duration.between(start, end == null ? LocalDateTime.now() : end).toMillis());
  }

  /** An execution's counts are the sums over its steps. */
  public static long total(List<Step> steps, ToLongFunction<Step> count) {
    return steps.stream().mapToLong(count).sum();
  }
}
