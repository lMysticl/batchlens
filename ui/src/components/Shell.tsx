import type { ReactNode } from "react";
import { BookOpen, ListTree, LogOut, Radio } from "lucide-react";

export function Shell({
  demo,
  onDisconnect,
  children,
}: {
  demo: boolean;
  onDisconnect: () => void;
  children: ReactNode;
}) {
  return (
    <div className={"application batch-theme"}>
      <aside className="sidebar">
        <a className="brand" href={"./index.html"}>
          <span className="brand-mark">B</span>
          <span>BatchLens</span>
        </a>
        <div className="sidebar-label">WORKSPACE</div>
        <nav aria-label="Workspace navigation">
          <a className="nav-item active" href="#workspace" aria-current="page">
            <ListTree size={18} />
            Executions
          </a>

          <a
            className="nav-item"
            href={"https://github.com/lMysticl/batchlens/blob/main/docs/usage.md"}
            target="_blank"
            rel="noreferrer"
          >
            <BookOpen size={18} />
            Documentation
          </a>
        </nav>
        <div className="sidebar-footer">
          <span className="version-tag">v0.1.0</span>
          <p>A clearer view of every batch.</p>
          <a href={"https://github.com/lMysticl/batchlens"} target="_blank" rel="noreferrer">
            View source ↗
          </a>
        </div>
      </aside>
      <div className="application-body">
        <header className="topbar">
          <div className="breadcrumb">
            <Radio size={14} />
            <span>BatchLens</span>
            <span className="slash">/</span>
            <span>Executions</span>
          </div>
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
            <LogOut size={17} />
          </button>
        </header>
        <main id="workspace">{children}</main>
      </div>
    </div>
  );
}
