import { useCallback, useState } from "react";
import { AlertCircle, Play, RefreshCw } from "lucide-react";
import type { Client } from "../api/client";
import type { Execution, Info, Job } from "../api/types";
import { ExecutionHistory } from "./ExecutionHistory";
import { ExecutionDetail } from "./ExecutionDetail";
import { StartExecution } from "./StartExecution";
import { usePolling } from "./usePolling";
import { label } from "./format";

export function JobWorkspace({ client, job, info }: { client: Client; job: Job; info: Info }) {
  const request = useCallback(
    (signal: AbortSignal) =>
      client.get<Execution[]>(`executions?job=${encodeURIComponent(job.name)}`, signal),
    [client, job.name],
  );
  const { data: executions, error, reload } = usePolling(request);
  const [selectedId, setSelectedId] = useState<string>();
  const [startOpen, setStartOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [notice, setNotice] = useState("");
  const [commandError, setCommandError] = useState("");
  const effectiveId = selectedId ?? executions?.[0]?.id;
  function accepted(execution: Execution) {
    setSelectedId(execution.id);
    setNotice(
      `Execution #${execution.id}: request accepted. The status below follows the repository.`,
    );
    setCommandError("");
    reload();
  }
  async function command(action: "restart" | "stop") {
    if (!effectiveId) return;
    setPending(true);
    setCommandError("");
    setNotice("");
    try {
      accepted(await client.post<Execution>(`executions/${effectiveId}/${action}`));
    } catch (failure) {
      setCommandError(failure instanceof Error ? failure.message : "Operation failed");
      reload();
    } finally {
      setPending(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">SPRING BATCH WORKSPACE</p>
          <h1>{label(job.name)}</h1>
          <p className="muted">
            Inspect committed work, understand a failure, continue from a checkpoint.
          </p>
        </div>
        <div className="heading-actions">
          <button className="button" onClick={reload} aria-label="Refresh executions">
            <RefreshCw size={14} />
            Refresh
          </button>
          <button
            className="button primary"
            onClick={() => setStartOpen(true)}
            disabled={!info.operationsEnabled || !job.registered}
          >
            <Play size={14} />
            Start new execution
          </button>
        </div>
      </div>
      {!info.operationsEnabled && (
        <div className="warning-banner">
          This application owner has enabled read-only access. Job operations are disabled.
        </div>
      )}
      {info.demo && (
        <div className="workspace-note">
          <span>LOCAL DEMO · 300 fixture invoices · the default run fails once at invoice 137</span>
          <span>Updates every 1.5s</span>
        </div>
      )}
      {error && (
        <div className="error-banner" role="alert">
          <AlertCircle size={16} />
          <span>Could not refresh: {error.message}. Showing the last successful result.</span>
          <button className="text-button" onClick={reload}>
            Retry
          </button>
        </div>
      )}
      {commandError && (
        <div className="error-banner" role="alert">
          {commandError}
        </div>
      )}
      {notice && (
        <div className="notice" role="status">
          {notice}
        </div>
      )}
      {executions === undefined ? (
        <div className="panel loading">Loading execution metadata…</div>
      ) : (
        <div className="execution-score">
          {effectiveId ? (
            <ExecutionDetail
              key={effectiveId}
              client={client}
              id={effectiveId}
              operationsEnabled={info.operationsEnabled && job.registered}
              pending={pending}
              onRestart={() => void command("restart")}
              onStop={() => void command("stop")}
            />
          ) : (
            <section className="panel empty-state">
              <h3>Your execution, in focus</h3>
              <p>Choose an execution to inspect its steps and parameters.</p>
            </section>
          )}
          <ExecutionHistory
            executions={executions}
            selectedId={effectiveId}
            onSelect={(id) => {
              setSelectedId(id);
              setNotice("");
            }}
          />
        </div>
      )}
      {startOpen && (
        <StartExecution
          demo={info.demo}
          onClose={() => setStartOpen(false)}
          onStart={(parameters) =>
            client.post<Execution>(`jobs/${encodeURIComponent(job.name)}/start`, { parameters })
          }
          onStarted={accepted}
        />
      )}
    </>
  );
}
