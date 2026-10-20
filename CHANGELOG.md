# Changelog

## 0.3.0 — 2026-10-20

- Add `batchlens-spring-boot3-starter` for Spring Boot 3.5 / Spring Batch 5.2 with the same
  console, REST API and properties. Tested with a real H2 job repository: authentication,
  start, restart of the same instance from its checkpoint, conflicts and missing executions.
- Both starters depend on the new `batchlens-core`, which holds the console, REST API and
  command rules once; each starter keeps only its Spring Batch adapter.
- The API and the console footer report the version stamped by the build.
- Release assets are built and checksummed by a tag-driven workflow.
- Documentation no longer contains launch drafts or process notes.

## 0.2.0 — 2026-10-20

- Porcelain & Vermilion: a new editorial workspace with horizontal navigation, distinct product identity and locally bundled OFL-licensed Geist typography.
- Connected execution routes, a numbered history ledger and a committed-work spread.
- Responsive keyboard-accessible layouts, visible focus, a skip link and product favicons.
- New repository covers, actual application captures and a documented design contract.
- Existing API and recovery semantics are preserved; v0.1.0 remains available.

## 0.1.0 — 2026-10-20

- Add an opt-in Spring Boot starter with an embedded React operations console.
- Inspect actual job/step metadata, parameters, counts and error details.
- Start/restart registered jobs through the host JobOperator.
- Preserve worker-owned checkpoints with cooperative stop signalling.
- Include a 300-invoice demo with transactional chunks and real restart recovery.
- Test authentication, conflicting commands, overload, business rollback and
  desktop/mobile journeys against the executable JAR.
- Include MIT licensing, dependency notices, pinned build inputs and CI on
  Ubuntu/Windows with Java 21/25.

The stop decision for the qualified Batch 6.0.5 integration is documented in
[the architecture record](docs/decisions/0001-cooperative-stop.md).
