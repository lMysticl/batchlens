import { useCallback } from "react";
import type { Client } from "../api/client";
import type { Execution } from "../api/types";
import { ExecutionInspector } from "./ExecutionInspector";
import { usePolling } from "./usePolling";

export function ExecutionDetail({
  client,
  id,
  operationsEnabled,
  pending,
  onRestart,
  onStop,
}: {
  client: Client;
  id: string;
  operationsEnabled: boolean;
  pending: boolean;
  onRestart: () => void;
  onStop: () => void;
}) {
  const request = useCallback(
    (signal: AbortSignal) => client.get<Execution>(`executions/${id}`, signal),
    [client, id],
  );
  const { data, error, reload } = usePolling(request);
  return (
    <div>
      {error && (
        <div className="error-banner" role="alert">
          {error.message}
          <button className="text-button" onClick={reload}>
            Retry
          </button>
        </div>
      )}
      {data ? (
        <ExecutionInspector
          execution={data}
          operationsEnabled={operationsEnabled}
          pending={pending}
          onRestart={onRestart}
          onStop={onStop}
        />
      ) : (
        <section className="panel loading">Loading execution #{id}…</section>
      )}
    </div>
  );
}
