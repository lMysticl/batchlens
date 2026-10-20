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
    <section className="panel" aria-labelledby="history-title">
      <div className="panel-heading">
        <div>
          <h2 id="history-title">Execution history</h2>
          <p className="panel-subtitle">Persisted by your Spring Batch repository</p>
        </div>
        <span className="section-caption">RECENT</span>
      </div>
      {executions.length === 0 ? (
        <div className="empty-state">
          <Inbox size={27} />
          <h3>No executions yet</h3>
          <p>Start a job to inspect its steps, parameters and committed work.</p>
        </div>
      ) : (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Execution</th>
                <th>Status</th>
                <th className="optional-column">Started</th>
                <th>Written</th>
                <th className="optional-column">Duration</th>
              </tr>
            </thead>
            <tbody>
              {executions.map((execution) => (
                <tr
                  key={execution.id}
                  className={execution.id === selectedId ? "selected" : undefined}
                >
                  <td>
                    <button
                      className="row-link mono"
                      onClick={() => onSelect(execution.id)}
                      aria-label={`Inspect execution ${execution.id}`}
                    >
                      #{execution.id}
                    </button>
                  </td>
                  <td>
                    <Status value={execution.status} />
                  </td>
                  <td className="optional-column numeric">{timestamp(execution.startTime)}</td>
                  <td className="numeric">{execution.writeCount.toLocaleString()}</td>
                  <td className="optional-column numeric">{duration(execution.durationMillis)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="table-footer">
        <span>
          {executions.length} recent execution{executions.length === 1 ? "" : "s"} · up to 20
          instances
        </span>
        <span>Live metadata</span>
      </div>
    </section>
  );
}
