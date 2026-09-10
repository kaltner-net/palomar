import { useState } from "react";
import { ForemanLogo } from "./ForemanLogo";
import type { ReleaseUpdateSnapshot } from "./protocol";
import type { ServerUpdateCheck, ServerUpdateOperation } from "./protocol";
import { componentUpdateStatus, type ComponentUpdateStatus } from "./update-status";
import { serverUpdatePhaseLabel, terminalUpdatePhases } from "./server-update";

export const FOREMAN_REPOSITORY_URL = "https://github.com/mkaltner/foreman";
export const FOREMAN_RELEASES_URL = `${FOREMAN_REPOSITORY_URL}/releases`;
export const FOREMAN_LICENSE_URL = `${FOREMAN_REPOSITORY_URL}/blob/main/LICENSE`;
export const FOREMAN_THIRD_PARTY_NOTICES_URL = `${FOREMAN_REPOSITORY_URL}/blob/main/THIRD_PARTY_NOTICES.md`;

export const WEB_CLIENT_VERSION = __FOREMAN_CLIENT_VERSION__;
export const WEB_CLIENT_COMMIT = __FOREMAN_CLIENT_COMMIT__;
export const WEB_RELEASE_BUILD = __FOREMAN_RELEASE_BUILD__;

const ABOUT_LINKS = [
  { label: "GitHub repository", href: FOREMAN_REPOSITORY_URL, icon: "github" },
  { label: "Current releases", href: FOREMAN_RELEASES_URL, icon: "release" },
  { label: "License", href: FOREMAN_LICENSE_URL, icon: "document" },
  { label: "Third-party notices", href: FOREMAN_THIRD_PARTY_NOTICES_URL, icon: "shield" },
] as const;

export interface AboutSectionProps {
  serverVersion: string | null;
  serverReleaseBuild?: boolean | null;
  connected: boolean;
  releaseUpdates?: ReleaseUpdateSnapshot | null;
  onCheckAgain?: () => Promise<void>;
  updateOperation?: ServerUpdateOperation | null;
  onReviewUpdate?: () => Promise<ServerUpdateCheck>;
  onStartUpdate?: () => Promise<ServerUpdateOperation>;
}

export function clientBuildDescription(version: string, commit: string, releaseBuild: boolean): string {
  const identity = releaseBuild ? version : `${version} (development build)`;
  return commit && commit !== "unknown" ? `${identity} · ${commit}` : identity;
}

function UpdateStatus({ status }: { status: ComponentUpdateStatus }) {
  return <div className={`about-update-status ${status.kind}`}>
    <strong>{status.label}</strong>
    {status.detail && <small>{status.detail}</small>}
    {status.release && <a href={status.release.releaseNotesUrl} target="_blank" rel="noreferrer noopener">
      Release notes for {status.release.tag}
    </a>}
  </div>;
}

function AboutLinkIcon({ icon }: { icon: (typeof ABOUT_LINKS)[number]["icon"] }) {
  const path = {
    github: "M12 2a10 10 0 0 0-3.16 19.49c.5.09.68-.22.68-.48v-1.87c-2.78.6-3.37-1.18-3.37-1.18-.45-1.16-1.11-1.47-1.11-1.47-.91-.62.07-.61.07-.61 1 .07 1.53 1.03 1.53 1.03.9 1.52 2.34 1.08 2.91.83.09-.64.35-1.08.63-1.33-2.22-.25-4.56-1.1-4.56-4.94 0-1.09.39-1.98 1.03-2.68-.1-.25-.45-1.27.1-2.64 0 0 .84-.27 2.75 1.02A9.6 9.6 0 0 1 12 6.84a9.6 9.6 0 0 1 2.5.34c1.91-1.29 2.75-1.02 2.75-1.02.55 1.37.2 2.39.1 2.64.64.7 1.03 1.59 1.03 2.68 0 3.85-2.34 4.68-4.57 4.93.36.31.68.92.68 1.85v2.75c0 .27.18.58.69.48A10 10 0 0 0 12 2Z",
    release: "M20 12v7a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-7h2v6h12v-6h2ZM11 3h2v9.17l2.59-2.58L17 11l-5 5-5-5 1.41-1.41L11 12.17V3Z",
    document: "M6 2h8l4 4v16H6V2Zm2 2v16h8V7h-3V4H8Zm2 7h4v2h-4v-2Zm0 4h4v2h-4v-2Z",
    shield: "m12 2 8 3v6c0 5.25-3.44 9.54-8 11-4.56-1.46-8-5.75-8-11V5l8-3Zm0 2.13L6 6.38V11c0 4.1 2.53 7.65 6 8.82 3.47-1.17 6-4.72 6-8.82V6.38l-6-2.25ZM11 7h2v6h-2V7Zm0 8h2v2h-2v-2Z",
  }[icon];
  return <svg viewBox="0 0 24 24" aria-hidden="true"><path d={path} /></svg>;
}

export function AboutSection({
  serverVersion,
  serverReleaseBuild = null,
  connected,
  releaseUpdates = null,
  onCheckAgain,
  updateOperation = null,
  onReviewUpdate,
  onStartUpdate,
}: AboutSectionProps) {
  const [checking, setChecking] = useState(false);
  const [checkError, setCheckError] = useState("");
  const [updateReview, setUpdateReview] = useState<ServerUpdateCheck | null>(null);
  const [updateBusy, setUpdateBusy] = useState(false);
  const [updateError, setUpdateError] = useState("");
  const browserMatchesServer = connected
    && serverVersion === WEB_CLIENT_VERSION
    && serverReleaseBuild === WEB_RELEASE_BUILD;
  const serverStatus = componentUpdateStatus(
    serverVersion,
    serverReleaseBuild,
    releaseUpdates,
    releaseUpdates?.components.server ?? null,
    "server",
  );
  const webStatus = componentUpdateStatus(
    WEB_CLIENT_VERSION,
    WEB_RELEASE_BUILD,
    releaseUpdates,
    releaseUpdates?.components.server ?? null,
    "server bundle",
  );
  const observedAt = releaseUpdates?.observedAt
    ? new Date(releaseUpdates.observedAt).toLocaleString()
    : null;
  const refreshUnavailable = releaseUpdates?.refreshStatus === "unavailable";
  return (
    <section className="settings-card about-card" aria-labelledby="about-heading">
      <div className="about-identity">
        <ForemanLogo large labelled />
        <div>
          <h2 id="about-heading">Foreman</h2>
          <p>Created by Michael Kaltner</p>
        </div>
      </div>
      <div className="about-component-list">
        <section aria-labelledby="server-version-heading">
          <h3 id="server-version-heading">
            {browserMatchesServer ? "Connected Foreman installation" : "Connected Foreman server"}
          </h3>
          <p className="about-installed">
            <span>{connected ? "Server" : "Last connected server"}</span>
            <strong>{serverVersion ?? "Unavailable"}</strong>
          </p>
          {browserMatchesServer && <p className="about-installed about-matching-build">
            <span>Bundled web client</span>
            <strong>{clientBuildDescription(WEB_CLIENT_VERSION, WEB_CLIENT_COMMIT, WEB_RELEASE_BUILD)} · matches server</strong>
          </p>}
          <UpdateStatus status={serverStatus} />
        </section>
        {!browserMatchesServer && <section aria-labelledby="web-version-heading">
          <h3 id="web-version-heading">This browser’s Foreman web client</h3>
          <p className="about-installed"><span>Build</span><strong>{clientBuildDescription(WEB_CLIENT_VERSION, WEB_CLIENT_COMMIT, WEB_RELEASE_BUILD)}</strong></p>
          <UpdateStatus status={webStatus} />
        </section>}
      </div>
      <div className="about-refresh">
        <p className="muted">
          {releaseUpdates?.stale && observedAt
            ? `Cached release information from ${observedAt}; the latest check is unavailable.`
            : refreshUnavailable && observedAt
              ? `Using release information observed ${observedAt}.`
              : observedAt
                ? `Release information checked ${observedAt}.`
                : "No validated release information is cached."}
        </p>
        {onCheckAgain && <button
          type="button"
          className="secondary"
          disabled={!connected || checking || releaseUpdates?.refreshStatus === "checking"}
          onClick={() => {
            setChecking(true);
            setCheckError("");
            void onCheckAgain()
              .catch(() => setCheckError("The update check is unavailable; cached information was kept."))
              .finally(() => setChecking(false));
          }}
        >{checking || releaseUpdates?.refreshStatus === "checking" ? "Checking…" : "Check again"}</button>}
        {checkError && <small role="status">{checkError}</small>}
      </div>
      {updateOperation && <div className={`server-update-progress ${updateOperation.phase}`} role="status">
        <div><strong>{serverUpdatePhaseLabel(updateOperation.phase)}</strong><span>{updateOperation.progress}%</span></div>
        <progress max="100" value={updateOperation.progress} />
        {updateOperation.message && <p>{updateOperation.message}</p>}
        {updateOperation.recoveryCommand && <p>From the server, run <code>{updateOperation.recoveryCommand}</code>.</p>}
      </div>}
      {serverStatus.kind === "update-available" && onReviewUpdate && (!updateOperation || (terminalUpdatePhases.has(updateOperation.phase) && updateOperation.phase !== "recoveryRequired")) && !updateReview && <button
        type="button"
        className="primary"
        disabled={!connected || updateBusy}
        onClick={() => {
          setUpdateBusy(true);
          setUpdateError("");
          void onReviewUpdate().then(setUpdateReview).catch((error) => setUpdateError(error instanceof Error ? error.message : "The update could not be reviewed.")).finally(() => setUpdateBusy(false));
        }}
      >{updateBusy ? "Reviewing…" : "Review server update"}</button>}
      {updateReview && <section className="server-update-review" aria-labelledby="server-update-review-heading">
        <h3 id="server-update-review-heading">Review server update</h3>
        <dl>
          <div><dt>Installed</dt><dd>{updateReview.currentVersion}</dd></div>
          <div><dt>Target</dt><dd>{updateReview.target?.version ?? "Unavailable"}</dd></div>
          <div><dt>Source</dt><dd><a href={updateReview.sourceUrl} target="_blank" rel="noreferrer noopener">{updateReview.source}</a></dd></div>
        </dl>
        {updateReview.target && <a href={updateReview.target.releaseNotesUrl} target="_blank" rel="noreferrer noopener">Read release notes for {updateReview.target.tag}</a>}
        {updateReview.blockers.length ? <div className="warning" role="alert"><strong>Update blocked</strong><p>{updateReview.blockers.map((blocker) => `${blocker.count} ${({ workingSession: "working session", waitingSession: "waiting session", pendingApproval: "pending approval", pendingInput: "pending input" } as const)[blocker.category]}${blocker.count === 1 ? "" : "s"}`).join(", ")} must finish first. No transcript content is shown.</p></div> : <p>No working, waiting, approval, or input state currently blocks activation.</p>}
        <p>Foreman will verify and stage the release, recheck session safety, restart only <code>foreman.service</code>, reconnect this client, and restore the previous payload automatically if health checking fails.</p>
        <div className="dialog-actions">
          <button type="button" className="secondary" onClick={() => setUpdateReview(null)} disabled={updateBusy}>Cancel</button>
          <button type="button" className="primary" disabled={updateBusy || !updateReview.updateAvailable || updateReview.blockers.length > 0} onClick={() => {
            if (!onStartUpdate) return;
            setUpdateBusy(true);
            setUpdateError("");
            void onStartUpdate().then(() => setUpdateReview(null)).catch((error) => setUpdateError(error instanceof Error ? error.message : "The update could not be started.")).finally(() => setUpdateBusy(false));
          }}>{updateBusy ? "Starting…" : "Install and restart"}</button>
        </div>
      </section>}
      {updateError && <p className="error-text" role="alert">{updateError}</p>}
      <p className="muted about-scope">Server updates install only signed official stable releases. Android APK installation remains a separate platform action.</p>
      <nav className="about-links" aria-label="Foreman links">
        {ABOUT_LINKS.map(({ label, href, icon }) => <a key={href} href={href} target="_blank" rel="noreferrer noopener">
          <AboutLinkIcon icon={icon} /><span>{label}</span>
        </a>)}
      </nav>
    </section>
  );
}
