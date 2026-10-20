# What actually committed? A small console for Spring Batch recovery

A job status tells you that an import failed. It does not tell you which work committed, which checkpoint the reader saved, or whether restarting the job will repeat records. Those are the questions BatchLens puts next to the restart button.

BatchLens is an opt-in Spring Boot starter with a bundled React interface. It reads the host application's Spring Batch repository and uses its registered jobs. There is no separate monitoring service to deploy. The starter is off by default; enabling it does not silently replace the host executor or security model.

![Actual execution workspace](assets/workspace.png)

## Try the failure before reading the architecture

Download the demo from [the v0.3.0 release](https://github.com/lMysticl/batchlens/releases/tag/v0.3.0) and run it with Java 21 or 25:

```sh
java -jar batchlens-demo-0.3.0.jar
```

Open `http://localhost:8091/batchlens/`, choose the bundled local demo token, and connect. Start the invoice import with the suggested parameters. It processes a fixture of 300 invoices and deliberately fails on the first attempt at invoice 137. With chunks of 20, the failed attempt has 120 committed writes. Open its step details and full error, then restart that execution. The second attempt writes the remaining 180 records; the business table ends with 300 unique invoice IDs.

The demo is intentionally a failure-and-recovery exercise. A fresh successful import proves less about checkpoint handling than a failed chunk followed by a correct restart.

## A stop request has a data ownership boundary

During implementation, a persistent-database browser run exposed a subtle problem in the qualified Spring Batch 6.0.5 stop path: metadata obtained by an HTTP request is a detached snapshot. Passing that snapshot into the operator's stop path could write stale step execution context while the worker was committing a chunk. A restart then began from an older reader position and hit the business table's primary key.

The fix was to signal `STOPPING` through the public `JobRepository` API and leave the step and checkpoint context to the worker. The UI reports that stop request and follows repository state; it does not manufacture completion. The [decision record](decisions/0001-cooperative-stop.md) describes the exact version and regression. This is a qualified implementation decision, not a universal claim about every Spring Batch release.

The tests cover rollback, competing restart requests, authenticated API access and bounded overload as well as the recovery path. An executor has two workers and a finite queue in the demo. The reusable starter leaves executor ownership with the host.

## What the UI deliberately does not calculate

The paper-and-ink workspace shows actual read, write, skip, commit and rollback counts. It does not display an estimated completion percentage when the host job has not supplied a total. The history ledger and connected route are alternate presentations of persisted execution data, rather than a second execution state machine in the browser.

The frontend uses typed requests and serial cancellable polling. Changing the selected execution aborts the previous request; a late response cannot replace the newly selected execution. Fonts, icons and the UI ship with the JAR.

## Where this fits

The April 2026 [request for a built-in Spring Batch dashboard](https://github.com/spring-projects/spring-batch/issues/5378) is a useful statement of the problem. BatchLens covers embedded inspection and manual start/stop/restart, but it does not implement the complete requested feature set: there is no date-range search, ETA, partition overview or abandon operation. It is independent of the Spring project and is not an official replacement for every previous administration product.

The current compatibility target is Spring Boot 4.1.1 / Spring Batch 6.0.5, one application instance, Java 21 or 25. CI exercises Ubuntu and Windows; the packaged browser journeys use Chromium on desktop and a mobile viewport. See the [validation map](validation.md) for reproducible commands and scope.

Source, runnable demo and MIT license: [github.com/lMysticl/batchlens](https://github.com/lMysticl/batchlens). Useful feedback is a reproducible host-job integration or an example where the execution view hides information needed for recovery.

*Publication disclosure: AI assistance was used in implementation and drafting. Behavior claims above have executable checks and retained release verification; this article should receive the author's final review before external publication.*
