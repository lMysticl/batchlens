<p align="center"><img src="docs/assets/cover.svg" alt="BatchLens: inspect and restart Spring Batch jobs" width="100%" /></p>

**See why a Spring Batch job failed. Restart it from its last saved checkpoint.**

Java 21 / 25 · Spring Boot · React · TypeScript · Self-hosted · MIT

[![Build and verify](https://github.com/lMysticl/batchlens/actions/workflows/ci.yml/badge.svg)](https://github.com/lMysticl/batchlens/actions/workflows/ci.yml)

[Download](https://github.com/lMysticl/batchlens/releases/latest) · [User guide](docs/usage.md) · [Architecture](docs/architecture.md)

![BatchLens showing a failed invoice import and its committed work](docs/assets/workspace.png)

_Real application capture from the included invoice demo._

## What it does

- Shows jobs, runs, failed steps and full error details.
- Shows how much work was read, written and committed.
- Stops a running job or restarts a failed or stopped run.

Runs inside your Spring Batch application. Spring Batch keeps ownership of job state and checkpoints.

## Try the demo

Requires **Java 21 or 25**. Download [batchlens-demo-0.2.0.jar](https://github.com/lMysticl/batchlens/releases/download/v0.2.0/batchlens-demo-0.2.0.jar), then run:

```sh
java -jar batchlens-demo-0.2.0.jar
```

1. Open [localhost:8091/batchlens](http://localhost:8091/batchlens/).
2. Choose **Use the bundled local demo token**, then **Connect workspace**.
3. Start a run with the suggested settings. When it fails, inspect the error and choose **Restart execution**.

The demo imports 300 sample invoices. It fails once at invoice 137, then resumes from the last committed chunk and finishes with 300 unique rows. Local data is saved in `./data/`.

## Use it in your application

The starter supports **Spring Boot 4.1.1 / Spring Batch 6.0.5**. Follow the [installation guide](docs/usage.md#host-application) to build it locally and configure your application. GitHub hosts this release; the starter is not on Maven Central.

Job controls are opt-in. v0.2.0 supports one application instance; see [operating limits](docs/usage.md#operating-limits) and [security](SECURITY.md) before deployment. The bundled demo token is public.

## For developers

[Build and test](CONTRIBUTING.md) · [Test coverage](docs/validation.md) · [Compatibility](docs/compatibility.md) · [Engineering brief](docs/project-brief.md) · [Design](docs/design.md) · [Technical article](docs/article.md)

Built by [Pavel Putrenkov](https://github.com/lMysticl). [MIT license](LICENSE).
