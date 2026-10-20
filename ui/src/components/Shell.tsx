import type { ReactNode } from "react";
import { LogOut } from "lucide-react";
import { Brand } from "./Brand";
export function Shell({
  demo,
  version,
  onDisconnect,
  children,
}: {
  demo: boolean;
  version: string;
  onDisconnect: () => void;
  children: ReactNode;
}) {
  return (
    <div className="application batch-theme">
      <a className="skip-link" href="#workspace">
        Skip to workspace
      </a>
      <header className="masthead">
        <a className="brand" href="./index.html" aria-label="BatchLens home">
          <Brand />
        </a>
        <span className="edition">/ execution score</span>
        <nav className="primary-nav" aria-label="Workspace navigation">
          <a className="nav-item active" href="#workspace" aria-current="page">
            Executions
          </a>
          <a
            className="nav-item"
            href="https://github.com/lMysticl/batchlens/blob/main/docs/usage.md"
            target="_blank"
            rel="noreferrer"
          >
            Field guide ↗
          </a>
        </nav>
        <div className="connection-label">
          <span className="connection-dot" />
          {demo ? "LOCAL DEMO" : "CONNECTED"}
        </div>
        <button
          className="icon-button"
          title="Disconnect"
          aria-label="Disconnect"
          onClick={onDisconnect}
        >
          <LogOut size={16} />
        </button>
      </header>
      <main id="workspace" tabIndex={-1}>
        {children}
      </main>
      <footer className="studio-footer">
        <span>KEEP BATCH MOVING FORWARD.</span>
        <a href="https://github.com/lMysticl/batchlens" target="_blank" rel="noreferrer">
          OPEN SOURCE / v{version} ↗
        </a>
      </footer>
    </div>
  );
}
