# Validation map

Run the documented checks from the correct directories in [CONTRIBUTING](../CONTRIBUTING.md).
The [CI workflow](../.github/workflows/ci.yml) runs the same Java/frontend gates
on Ubuntu and Windows with Java 21/25. Test counts below describe unique test
cases, not the sum of repeated matrix executions.

| Gate | Evidence |
|---|---|
| Core rules, 11 JVM tests | Read-only switch, parameter limits, restart and stop preconditions, newest-first history and its limits, token length, build-stamped version; run against an in-memory adapter |
| Opt-in starters, 1 JVM test each | Disabled integration installs no endpoints and needs no Batch infrastructure |
| Actual HTTP + Batch, 7 JVM tests | Real failed chunk restart to 300 unique rows; cooperative stop/resume; concurrent restart conflict; input/error contracts; encoded/matrix path authentication; bounded executor overload; real business transaction rollback |
| Spring Boot 3.5 starter, 4 JVM tests | Real H2 job repository: 401 without token, failed run restarted as the same instance with only the unfinished step re-run, 409 on restart/stop of a completed run, 404/400 contracts |
| React, 4 tests | String parameter validation; aborted stale requests; recoverable polling failure |
| Production assets | TypeScript strict checking, Prettier, Vite build, embedded UI in the executable JAR |
| Java quality | Compiler deprecation warnings treated as errors; Spotless formatting |
| Browser journey | Built JAR, actual job commands, details, keyboard dialog, desktop/mobile layout |

The failure in the invoice fixture is intentional. A successful recovery test
must observe committed rows before restart, then exactly 300 distinct invoice IDs.
Passing a mocked JobOperator would not prove this behavior.

GitHub release notes link the exact commit and CI runs. Downloadable assets have
SHA256 checksums. The release verification record distinguishes local checks,
CI and public-asset smoke tests. No load, multi-node or universal compatibility
claim follows from these checks.

Run the committed desktop/mobile browser scenarios with `npm run test:browser` from
`ui/` after building the JAR. Install the matching Chromium with `npx playwright install chromium`.
