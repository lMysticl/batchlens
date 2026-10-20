package io.github.lmysticl.batchlens.demo;

import static org.junit.jupiter.api.Assertions.*;

import io.github.lmysticl.batchlens.catalog.BatchViews;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:batchlens-contract;DB_CLOSE_DELAY=-1")
class BatchLensIntegrationTest {
  private static final String TOKEN = "local-demo-token-change-this-before-deployment";
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Value("${local.server.port}")
  int port;

  @Autowired JdbcTemplate jdbc;
  @Autowired PlatformTransactionManager transactions;

  @Autowired
  @Qualifier("batchExecutor")
  ThreadPoolTaskExecutor executor;

  @BeforeEach
  void isolateScenariosFromQueuedTaskCleanup() throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
    while (executor.getActiveCount() != 0
        || !executor.getThreadPoolExecutor().getQueue().isEmpty()) {
      if (System.nanoTime() >= deadline) fail("Previous scenario left active or queued work");
      Thread.sleep(25);
    }
  }

  @AfterAll
  static void closeClient() {
    HTTP.close();
  }

  @Test
  void failedImportResumesFromTheCommittedCheckpoint() throws Exception {
    String run = UUID.randomUUID().toString();
    var failed = awaitTerminal(start(run, "137", "0").id());
    assertEquals("FAILED", failed.status());
    long committed = rows(run);
    assertTrue(committed > 0 && committed < 300, "There must be a real committed checkpoint");
    assertTrue(
        failed.steps().stream().anyMatch(step -> step.error().contains("Planned demo failure")));
    var response = send("POST", "/executions/" + failed.id() + "/restart", null, true);
    assertEquals(202, response.statusCode(), response.body());
    var completed = awaitTerminal(execution(response).id());
    assertEquals("COMPLETED", completed.status());
    assertEquals(300, rows(run));
    assertEquals(300 - committed, completed.writeCount());
    assertEquals(failed.instanceId(), completed.instanceId());
    assertNotEquals(failed.id(), completed.id());
    assertEquals(
        300L,
        jdbc.queryForObject(
            "SELECT COUNT(DISTINCT invoice_id) FROM demo_invoice WHERE run_key=?",
            Long.class,
            run));
  }

  @Test
  void stopIsCooperativeAndStoppedExecutionCanResume() throws Exception {
    String run = UUID.randomUUID().toString();
    var started = start(run, "0", "15");
    assertTrue(
        Set.of("STARTING", "STARTED").contains(started.status()),
        "Start must return before the job finishes");
    long checkpointDeadline = System.nanoTime() + Duration.ofSeconds(8).toNanos();
    while (rows(run) < 20 && System.nanoTime() < checkpointDeadline) Thread.sleep(10);
    assertTrue(rows(run) >= 20, "Stop must interrupt an import with committed work");
    var stop = send("POST", "/executions/" + started.id() + "/stop", null, true);
    assertEquals(202, stop.statusCode(), stop.body());
    var stopped = awaitTerminal(started.id());
    assertEquals("STOPPED", stopped.status());
    assertEquals(
        409, send("POST", "/executions/" + stopped.id() + "/stop", null, true).statusCode());
    var resumed = send("POST", "/executions/" + stopped.id() + "/restart", null, true);
    assertEquals(202, resumed.statusCode(), resumed.body());
    assertEquals("COMPLETED", awaitTerminal(execution(resumed).id()).status());
    assertEquals(300, rows(run));
  }

  @Test
  void competingRestartCommandsCannotRunTheSameInstanceTwice() throws Exception {
    var failed = awaitTerminal(start(UUID.randomUUID().toString(), "41", "2").id());
    assertEquals("FAILED", failed.status());
    assertTrue(
        failed.steps().stream().anyMatch(step -> step.error().contains("Planned demo failure")));
    var request = request("POST", "/executions/" + failed.id() + "/restart", null, true);
    var first = HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    var second = HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    CompletableFuture.allOf(first, second).get();
    var responses = List.of(first.get(), second.get());
    assertEquals(
        List.of(202, 409), responses.stream().map(HttpResponse::statusCode).sorted().toList());
    var accepted =
        responses.stream()
            .filter(response -> response.statusCode() == 202)
            .findFirst()
            .orElseThrow();
    assertEquals("COMPLETED", awaitTerminal(execution(accepted).id()).status());
  }

  @Test
  void apiRejectsUnauthenticatedAndInvalidOperations() throws Exception {
    assertEquals(401, send("GET", "/jobs", null, false).statusCode());
    assertEquals(
        400, send("GET", "/executions?job=invoiceImport&limit=0", null, true).statusCode());
    assertEquals(404, send("GET", "/executions/99999999", null, true).statusCode());
    assertEquals(
        404, send("POST", "/jobs/absent/start", Map.of("parameters", Map.of()), true).statusCode());
    var invalid =
        send(
            "POST",
            "/jobs/invoiceImport/start",
            Map.of("parameters", Map.of("not a name", "x")),
            true);
    assertEquals(400, invalid.statusCode(), invalid.body());
    assertFalse(send("GET", "/info", null, true).body().contains(TOKEN));
  }

  @Test
  void encodedAndMatrixPathsDoNotBypassAuthentication() throws Exception {
    for (String route : List.of("/%62atchlens/api/jobs", "/batchlens/api/jobs;view=full")) {
      var request =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route)).GET().build();
      assertEquals(
          401, HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode(), route);
    }
  }

  @Test
  void executorOverloadIsBoundedAndItsRejectedExecutionRemainsInspectable() throws Exception {
    var accepted = new ArrayList<BatchViews.Execution>();
    try {
      for (int index = 0; index < 7; index++)
        accepted.add(start(UUID.randomUUID().toString(), "0", "100"));
      var rejected = awaitTerminal(accepted.getLast().id());
      assertEquals("FAILED", rejected.status());
      assertTrue(rejected.error().contains("TaskRejectedException"), rejected.error());
      assertEquals(0, rejected.writeCount());
      assertTrue(rejected.steps().isEmpty());
    } finally {
      for (var execution : accepted) {
        var current = execution(send("GET", "/executions/" + execution.id(), null, true));
        if (Set.of("STARTING", "STARTED").contains(current.status()))
          send("POST", "/executions/" + execution.id() + "/stop", null, true);
      }
      for (var execution : accepted) awaitTerminal(execution.id());
    }
  }

  @Test
  void hostTransactionManagerRollsBackBusinessWrites() {
    String run = UUID.randomUUID().toString();
    assertThrows(
        IllegalStateException.class,
        () ->
            new TransactionTemplate(transactions)
                .executeWithoutResult(
                    status -> {
                      jdbc.update(
                          "INSERT INTO demo_invoice (run_key,invoice_id,customer,amount) VALUES (?,?,?,?)",
                          run,
                          999,
                          "rollback probe",
                          1);
                      throw new IllegalStateException("rollback the owned probe");
                    }));
    assertEquals(0, rows(run), transactions.getClass().getName());
  }

  private BatchViews.Execution start(String run, String failAt, String delay) throws Exception {
    var response =
        send(
            "POST",
            "/jobs/invoiceImport/start",
            Map.of("parameters", Map.of("run", run, "failAt", failAt, "delayMs", delay)),
            true);
    assertEquals(202, response.statusCode(), response.body());
    return execution(response);
  }

  private BatchViews.Execution awaitTerminal(String id) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
    while (System.nanoTime() < deadline) {
      var current = execution(send("GET", "/executions/" + id, null, true));
      if (!Set.of("STARTING", "STARTED", "STOPPING").contains(current.status())) return current;
      Thread.sleep(40);
    }
    throw new AssertionError("Execution did not reach a terminal state: " + id);
  }

  private long rows(String run) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM demo_invoice WHERE run_key=?", Long.class, run);
  }

  private static BatchViews.Execution execution(HttpResponse<String> response) {
    assertTrue(response.statusCode() < 300, response.body());
    return JSON.readValue(response.body(), BatchViews.Execution.class);
  }

  private HttpResponse<String> send(String method, String route, Object body, boolean authenticated)
      throws Exception {
    return HTTP.send(
        request(method, route, body, authenticated), HttpResponse.BodyHandlers.ofString());
  }

  private HttpRequest request(String method, String route, Object body, boolean authenticated) {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/batchlens/api" + route))
            .timeout(Duration.ofSeconds(10));
    if (authenticated) request.header("Authorization", "Bearer " + TOKEN);
    if (body != null) request.header("Content-Type", "application/json");
    return request
        .method(
            method,
            body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
        .build();
  }
}
