# Using BatchLens

## Demo workflow

Start the release JAR and open `http://localhost:8091/batchlens/`. Connect using
the public demo token shortcut. Choose **Start new execution**. The default
parameters create a unique run, fail at invoice 137 on the first attempt, and
delay each item by 8 ms so progress is visible.

The three real steps are `validateInput`, `importInvoices` and `reconcileInvoices`.
The import commits chunks of 20 rows. A failure leaves committed rows in the
business table and the reader's checkpoint in Batch metadata. Restart uses the
same job instance and parameters, with a new execution ID. It does not reinsert
the committed rows. The business table has a primary key, so duplicates fail
rather than being hidden by an upsert.

Set `failAt` to `"0"` for an uninterrupted run. `delayMs` accepts 0–200. `failAt`
accepts 0–300. A new import needs a new `run` key. Stop is cooperative: the
current chunk may finish before the repository reaches STOPPED.

## Host application

v0.2.0 supports **Spring Boot 4.1.1 / Spring Batch 6.0.5**. Build and install
from source; this release is hosted on GitHub and is not on Maven Central.
Requires JDK 21 or 25, Git and Node 24.15+ (24.x).

```sh
git clone https://github.com/lMysticl/batchlens.git
cd batchlens/ui
npm ci
npm run build
cd ..
./mvnw -B install
```

On Windows, use `mvnw.cmd`. Add the starter to your application:

```xml
<dependency>
  <groupId>io.github.lmysticl</groupId>
  <artifactId>batchlens-spring-boot-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

Use the tested Boot/Batch versions, a configured JDBC `JobRepository`, datasource,
transaction manager and registered `Job` beans. Set `batchlens.enabled=true` and
a token of at least 32 characters. The UI is available at `/batchlens/`; API
calls use `/batchlens/api/`. Host security, servlet context paths and network
policy still apply. The UI resolves its API relative to its own page.

Start with read-only access:

```properties
batchlens.enabled=true
batchlens.api-token=${BATCHLENS_API_TOKEN}
batchlens.operations-enabled=false
```

Job control uses the host's `JobOperator`. Configure a bounded asynchronous
executor before enabling `batchlens.operations-enabled=true`. For example:

```java
@Bean
@BatchTaskExecutor
ThreadPoolTaskExecutor batchExecutor() {
  var executor = new ThreadPoolTaskExecutor();
  executor.setCorePoolSize(2);
  executor.setMaxPoolSize(2);
  executor.setQueueCapacity(4);
  executor.setThreadNamePrefix("batch-work-");
  executor.setWaitForTasksToCompleteOnShutdown(true);
  executor.setAwaitTerminationSeconds(10);
  return executor;
}
```

Imports: `org.springframework.context.annotation.Bean`,
`org.springframework.boot.batch.autoconfigure.BatchTaskExecutor` and
`org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor`.
Choose capacity for your application's workload. The demo uses this same bounded
configuration. BatchLens does not replace the host's executor or job policy.

## API

Every API request requires `Authorization: Bearer <token>`. Errors use HTTP
Problem Details. IDs are strings in JSON; keep them as strings in clients.

| Method | Relative to `/batchlens/api`             | Result                                                        |
| ------ | ---------------------------------------- | ------------------------------------------------------------- |
| GET    | `/info`                                  | Version, demo and operation configuration                     |
| GET    | `/jobs`                                  | Registered and historical job names                           |
| GET    | `/executions?job=invoiceImport&limit=20` | Recent executions                                             |
| GET    | `/executions/{id}`                       | Execution, parameters and steps                               |
| POST   | `/jobs/{name}/start`                     | Accepted execution; body `{"parameters":{"run":"example-1"}}` |
| POST   | `/executions/{id}/restart`               | Accepted restart of FAILED/STOPPED execution                  |
| POST   | `/executions/{id}/stop`                  | Accepted cooperative stop                                     |

Parameters are identifying strings: at most 20, with names up to 64 characters
and values up to 1,024 characters. Numeric, date and non-identifying parameter
types are outside this release. History searches the 20 newest instances and
returns at most the requested limit, sorted by execution ID. This is a recent
operations view, not a historical export API.

401 means the token is absent or wrong. 403 means operations are disabled.
404 means a job or execution is unavailable. 409 means current Batch state
does not allow the command. Refresh before deciding what to do next.

## Operating limits

Use one application instance for command control in v0.2.0. The striped command
locks coordinate requests in this JVM; the Batch repository remains authoritative.
The demo executor has two workers and four queued tasks. The host decides what
to do when its executor is full. In the demo, excess submissions remain inspectable
as FAILED executions with TaskRejectedException in the exit detail. No hard-kill endpoint is provided.

A process crash can leave a Spring Batch execution marked STARTED. BatchLens
does not automatically rewrite or restart that metadata; follow the host
application's reviewed recovery procedure. H2 is the qualified demo database.
Other databases and versions need separate qualification.
