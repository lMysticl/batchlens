import { useState } from "react";
import type { FormEvent } from "react";
import { Play } from "lucide-react";
import { Dialog } from "../components/Dialog";
import type { Execution } from "../api/types";

export function parseParameters(value: string): Record<string, string> {
  const parsed: unknown = JSON.parse(value);
  if (
    parsed === null ||
    Array.isArray(parsed) ||
    typeof parsed !== "object" ||
    Object.values(parsed).some((item) => typeof item !== "string")
  ) {
    throw new Error('Parameters must be a JSON object of strings, for example {"run":"import-1"}.');
  }
  return parsed as Record<string, string>;
}

export function StartExecution({
  demo,
  onClose,
  onStart,
  onStarted,
}: {
  demo: boolean;
  onClose: () => void;
  onStart: (parameters: Record<string, string>) => Promise<Execution>;
  onStarted: (execution: Execution) => void;
}) {
  const [parameters, setParameters] = useState(() =>
    JSON.stringify(
      demo
        ? { run: `demo-${crypto.randomUUID().slice(0, 8)}`, failAt: "137", delayMs: "8" }
        : { run: `run-${crypto.randomUUID().slice(0, 8)}` },
      null,
      2,
    ),
  );
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setPending(true);
    try {
      const execution = await onStart(parseParameters(parameters));
      onStarted(execution);
      onClose();
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : "Could not start execution");
    } finally {
      setPending(false);
    }
  }
  return (
    <Dialog title="Start a new execution" onClose={onClose}>
      <form onSubmit={(event) => void submit(event)}>
        <label htmlFor="parameters">Job parameters</label>
        <textarea
          id="parameters"
          value={parameters}
          onChange={(event) => setParameters(event.target.value)}
          disabled={pending}
          spellCheck={false}
          autoFocus
        />
        <p className="field-help">
          All values are strings and identify the job instance. Use a new run key for a fresh
          import.
          {demo && (
            <>
              {" "}
              The demo fails once at invoice 137; restarting it continues from the committed
              checkpoint. Set failAt to "0" for an uninterrupted run.
            </>
          )}
        </p>
        {error && (
          <div className="error-banner" role="alert">
            {error}
          </div>
        )}
        <div className="modal-actions">
          <button className="button" type="button" onClick={onClose} disabled={pending}>
            Cancel
          </button>
          <button className="button primary" disabled={pending}>
            <Play size={14} />
            {pending ? "Starting…" : "Start execution"}
          </button>
        </div>
      </form>
    </Dialog>
  );
}
