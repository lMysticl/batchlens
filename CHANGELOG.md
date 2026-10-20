# Changelog

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
