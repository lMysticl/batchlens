import { Inbox } from "lucide-react";
import type { Execution } from "../api/types";
import { Status } from "../components/Status";
import { duration, timestamp } from "./format";

export function ExecutionHistory({
  executions,
  selectedId,
  onSelect,
}: {
  executions: Execution[];
  selectedId: string | undefined;
  onSelect: (id: string) => void;
}) {
  return (
    <section className="execution-ledger" aria-labelledby="history-title">
      <div className="history-caption">
        <h2 id="history-title">Execution history</h2>
        <p>
          Each run has its own record.
          <br />
          Choose one to inspect.
        </p>
        <span className="section-caption">{executions.length} RECENT · UP TO 20 INSTANCES</span>
      </div>
      {executions.length === 0 ? (
        <div className="empty-state">
          <Inbox size={24} />
          <h3>No executions yet</h3>
          <p>Start a job to inspect its steps and committed work.</p>
        </div>
      ) : (
        <ol className="execution-timeline">
          {executions.map((execution) => (
            <li key={execution.id}>
              <button
                aria-label={`Inspect execution ${execution.id}`}
                aria-pressed={execution.id === selectedId}
                onClick={() => onSelect(execution.id)}
              >
                <strong>#{execution.id}</strong>
                <Status value={execution.status} />
                <time>{timestamp(execution.startTime)}</time>
                <span>
                  {execution.writeCount.toLocaleString()} written ·{" "}
                  {duration(execution.durationMillis)}
                </span>
              </button>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
