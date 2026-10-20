package io.github.lmysticl.batchlens.web;

import io.github.lmysticl.batchlens.BatchLensProperties;
import io.github.lmysticl.batchlens.catalog.BatchCatalog;
import io.github.lmysticl.batchlens.catalog.BatchViews;
import io.github.lmysticl.batchlens.operations.BatchCommands;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/batchlens/api")
public final class BatchController {
  private final BatchCatalog catalog;
  private final BatchCommands commands;
  private final BatchLensProperties properties;
  private final Environment environment;

  public BatchController(
      BatchCatalog catalog,
      BatchCommands commands,
      BatchLensProperties properties,
      Environment environment) {
    this.catalog = catalog;
    this.commands = commands;
    this.properties = properties;
    this.environment = environment;
  }

  @GetMapping("/info")
  public BatchViews.Info info() {
    return new BatchViews.Info(
        "BatchLens",
        "0.3.0",
        properties.operationsEnabled(),
        environment.getProperty("batchlens.demo", Boolean.class, false));
  }

  @GetMapping("/jobs")
  public List<BatchViews.Job> jobs() {
    return catalog.jobs();
  }

  @GetMapping("/executions")
  public List<BatchViews.Execution> executions(
      @RequestParam String job, @RequestParam(defaultValue = "20") int limit) {
    return catalog.executions(job, limit);
  }

  @GetMapping("/executions/{id}")
  public BatchViews.Execution execution(@PathVariable long id) {
    return catalog.execution(id);
  }

  @PostMapping("/jobs/{name}/start")
  public ResponseEntity<BatchViews.Execution> start(
      @PathVariable String name, @RequestBody StartRequest request) {
    return ResponseEntity.accepted().body(commands.start(name, request.parameters()));
  }

  @PostMapping("/executions/{id}/restart")
  public ResponseEntity<BatchViews.Execution> restart(@PathVariable long id) {
    return ResponseEntity.accepted().body(commands.restart(id));
  }

  @PostMapping("/executions/{id}/stop")
  public ResponseEntity<BatchViews.Execution> stop(@PathVariable long id) {
    return ResponseEntity.accepted().body(commands.stop(id));
  }

  public record StartRequest(Map<String, String> parameters) {}
}
