import { useCallback, useMemo, useState } from "react";
import { createClient } from "./api/client";
import type { Info, Job } from "./api/types";
import { Login } from "./components/Login";
import { Shell } from "./components/Shell";
import { JobWorkspace } from "./workspace/JobWorkspace";
import { usePolling } from "./workspace/usePolling";

function Connected({
  token,
  info,
  disconnect,
}: {
  token: string;
  info: Info;
  disconnect: () => void;
}) {
  const client = useMemo(() => createClient(token), [token]);
  const request = useCallback((signal: AbortSignal) => client.get<Job[]>("jobs", signal), [client]);
  const { data: jobs, error, reload } = usePolling(request, 15000);
  const [selection, setSelection] = useState<string>();
  const job = jobs?.find((item) => item.name === selection) ?? jobs?.[0];
  return (
    <Shell demo={info.demo} version={info.version} onDisconnect={disconnect}>
      {jobs && jobs.length > 1 && (
        <div className="job-list" aria-label="Registered jobs">
          {jobs.map((item) => (
            <button
              key={item.name}
              aria-pressed={item.name === job?.name}
              onClick={() => setSelection(item.name)}
            >
              {item.name}
            </button>
          ))}
        </div>
      )}
      {error && (
        <div className="error-banner" role="alert">
          {error.message}
          <button className="text-button" onClick={reload}>
            Retry
          </button>
        </div>
      )}
      {job ? (
        <JobWorkspace key={job.name} client={client} job={job} info={info} />
      ) : (
        <div className="panel empty-state">
          <h3>{jobs ? "No registered or recorded jobs" : "Loading jobs…"}</h3>
          <p>
            {jobs
              ? "Declare a Spring Batch Job in this application to get started."
              : "Connecting to the Batch repository."}
          </p>
        </div>
      )}
    </Shell>
  );
}

export default function App() {
  const [session, setSession] = useState<{ token: string; info: Info }>();
  async function connect(token: string) {
    const info = await createClient(token).get<Info>("info");
    sessionStorage.setItem("batchlens-token", token);
    setSession({ token, info });
  }
  function disconnect() {
    sessionStorage.removeItem("batchlens-token");
    setSession(undefined);
  }
  return session ? (
    <Connected token={session.token} info={session.info} disconnect={disconnect} />
  ) : (
    <Login initialToken={sessionStorage.getItem("batchlens-token") ?? ""} onConnect={connect} />
  );
}
