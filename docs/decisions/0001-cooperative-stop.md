# Cooperative stop owns a signal, not a checkpoint

Status: accepted for the qualified Spring Batch 6.0.5 integration.

## Observation

A browser scenario stopped an invoice import during a chunk, then restarted it.
The first execution had committed rows, but its persisted reader checkpoint was
zero. Restart attempted to insert an existing primary key and failed. The same
problem was reproduced in a focused HTTP integration test after waiting for at
least one committed chunk. A separate transaction rollback probe passed, ruling
out a mismatched business transaction manager.

The qualified implementation of
[`SimpleJobOperator.stop(JobExecution)`](https://github.com/spring-projects/spring-batch/blob/v6.0.5/spring-batch-core/src/main/java/org/springframework/batch/core/launch/support/SimpleJobOperator.java)
persists step execution contexts from its supplied execution snapshot. That
snapshot can be older than the checkpoint committed by the worker. Its stop path
also marks an end time before the running chunk has settled.

## Decision

Start and restart still use the host `JobOperator`. Stop changes the selected
execution to STOPPING using the public `JobRepository.update` API. It does not
set an end time or update step contexts. Spring Batch's worker observes that
signal at a chunk boundary and owns the final step state and checkpoint.

No Batch table SQL, custom schema or dependency fork is required. A concurrent
metadata-version change becomes a conflict response instead of an overwrite.
Commands remain scoped to the selected execution and the configured read-only
switch still applies.

## Verification

The regression first failed with a duplicate invoice after stop/resume. With
the cooperative signal it completes with 300 unique rows. The full HTTP suite
also covers failed-chunk recovery, competing restarts and executor overload.
The committed desktop/mobile browser scenarios exercise stop followed by restart
against the packaged JAR. A newer Batch release needs separate qualification;
this decision does not claim every version has the same behavior.
