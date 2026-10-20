# Engineering brief

BatchLens is an open-source Java and React project by [Pavel Putrenkov](https://github.com/lMysticl).

**Problem.** A failed batch job needs an operator to identify committed work and restart the correct execution without editing metadata.

**Implementation.** Java 21, Spring Boot 4.1, Spring Batch 6, JDBC metadata, an opt-in auto-configuration starter, React 19, TypeScript, Vite, REST APIs and bearer-token access.

**Engineering decisions.** Registered Job beans own execution. JobRepository owns history. A bounded task executor keeps operation requests asynchronous; command conflicts are explicit.

**Evidence.** Tests and reproducible demo scenarios are listed in
[validation](validation.md). No production adoption, customer count, traffic,
revenue or performance improvement is claimed for this new project.

**Portfolio wording.** Built BatchLens, a self-hosted Spring Batch operations starter with a React/TypeScript UI; implemented checkpoint recovery and asynchronous job control and verified the behavior with integration tests and CI on Java 21/25, Linux and Windows.

This wording describes the project. It is not a claim of commercial employment or production scale.
