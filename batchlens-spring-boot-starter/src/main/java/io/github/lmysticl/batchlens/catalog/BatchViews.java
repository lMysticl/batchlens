package io.github.lmysticl.batchlens.catalog;

import java.util.List;
import java.util.Map;

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
}
