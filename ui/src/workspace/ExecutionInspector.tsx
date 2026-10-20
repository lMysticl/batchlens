import { useState } from "react";
import { FileText, RotateCcw, Square } from "lucide-react";
import type { Execution } from "../api/types";
import { Status } from "../components/Status";
import { duration, label, running, timestamp } from "./format";

export function ExecutionInspector({
  execution,
  operationsEnabled,
  pending,
  onRestart,
  onStop,
}: {
  execution: Execution;
  operationsEnabled: boolean;
  pending: boolean;
  onRestart: () => void;
  onStop: () => void;
}) {
  const [tab, setTab] = useState("steps");
  const failure =
    execution.steps.find((step) => step.status === "FAILED")?.error || execution.error;
  const summary =
    failure.split("\n").find((line) => line.includes("Planned demo failure")) ||
    failure.split("\n")[0];
  return (
    <section className="panel" aria-labelledby="execution-title">
      <div className="panel-heading">
        <div>
          <div className="inspector-title">
            <h2 id="execution-title">Execution #{execution.id}</h2>
            <span className="badge">
              <Status value={execution.status} />
            </span>
          </div>
          <div className="inspector-meta">
            <span>{timestamp(execution.startTime || execution.createTime)}</span>
            <span>{duration(execution.durationMillis)}</span>
            <span>Instance #{execution.instanceId}</span>
          </div>
        </div>
      </div>
      <div className="tabs" aria-label="Execution details">
        <button aria-pressed={tab === "steps"} onClick={() => setTab("steps")}>
          Steps
        </button>
        <button aria-pressed={tab === "error"} onClick={() => setTab("error")}>
          Error details
        </button>
        <button aria-pressed={tab === "parameters"} onClick={() => setTab("parameters")}>
          Parameters
        </button>
      </div>
      {tab === "steps" && (
        <>
          <div className="step-list">
            {execution.steps.length === 0 && (
              <div className="loading">Waiting for the first step…</div>
            )}
            {execution.steps.map((step, index) => (
              <div className="step-item" data-status={step.status} key={step.id}>
                <span className="step-number">{index + 1}</span>
                <div>
                  <p className="step-name">{label(step.name)}</p>
                  <Status value={step.status} />
                  <div className="step-detail">
                    <span>{step.commitCount} commits</span>
                    <span>{step.rollbackCount} rollbacks</span>
                    <span>{step.skipCount} skipped</span>
                  </div>
                </div>
                <div className="step-summary">
                  <strong>
                    {step.readCount} / {step.writeCount}
                  </strong>
                  <br />
                  read / written
                  <br />
                  {duration(step.durationMillis)}
                </div>
              </div>
            ))}
          </div>
          <div className="execution-report">
            <dl className="work-counts">
              <div className="principal-count">
                <dt>Committed writes in this execution</dt>
                <dd data-testid="execution-written-count">{execution.writeCount}</dd>
              </div>
              <div>
                <dt>Records read</dt>
                <dd>{execution.readCount}</dd>
              </div>
              <div>
                <dt>Step commits</dt>
                <dd>{execution.steps.reduce((sum, step) => sum + step.commitCount, 0)}</dd>
              </div>
              <div>
                <dt>Step rollbacks</dt>
                <dd>{execution.steps.reduce((sum, step) => sum + step.rollbackCount, 0)}</dd>
              </div>
            </dl>
            {failure ? (
              <div className="failure-callout">
                <p className="eyebrow">EXECUTION NEEDS ATTENTION</p>
                <p>
                  {summary?.slice(0, 250)}
                  {summary && summary.length > 250 ? "…" : ""}
                </p>
                <button className="text-button" onClick={() => setTab("error")}>
                  Read the full error →
                </button>
              </div>
            ) : (
              <p className="execution-note">
                Counts follow committed repository state. A restart continues the same job instance
                from its saved checkpoint.
              </p>
            )}
          </div>
        </>
      )}
      {tab === "error" && (
        <div className="panel-body">
          {failure ? (
            <pre className="code-block error-details">{failure}</pre>
          ) : (
            <div className="empty-state">
              <FileText size={24} />
              <h3>No recorded error</h3>
              <p>This execution has no failure description.</p>
            </div>
          )}
        </div>
      )}
      {tab === "parameters" && (
        <div className="panel-body">
          <pre className="code-block">{JSON.stringify(execution.parameters, null, 2)}</pre>
          <p className="field-help" style={{ marginBottom: 0 }}>
            These identifying parameters belong to job instance #{execution.instanceId}.
          </p>
        </div>
      )}
      <div className="inspector-actions">
        {running(execution.status) && (
          <button
            className="button danger"
            disabled={!operationsEnabled || pending || execution.status === "STOPPING"}
            onClick={onStop}
          >
            <Square size={13} />
            {execution.status === "STOPPING" ? "Stopping at a safe boundary…" : "Stop execution"}
          </button>
        )}
        {["FAILED", "STOPPED"].includes(execution.status) && (
          <button
            className="button primary"
            disabled={!operationsEnabled || pending}
            onClick={onRestart}
          >
            <RotateCcw size={14} />
            {pending ? "Submitting…" : "Restart execution"}
          </button>
        )}
        {!running(execution.status) && !["FAILED", "STOPPED"].includes(execution.status) && (
          <span className="panel-subtitle">
            This execution is {execution.status.toLowerCase()}.
          </span>
        )}
      </div>
    </section>
  );
}
