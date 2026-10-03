import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import BeaconsList from './BeaconsList';
import {
  createBeacon,
  deleteBeacon,
  listBeacons,
  recordTestVisit,
  renameBeacon
} from './api';
import './tokens.css';
import './Workbench.css';

const REFRESH_INTERVAL_MS = 15000;

function App() {
  const [beacons, setBeacons] = useState([]);
  const [searchString, setSearchString] = useState('');
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState(null);
  const searchRef = useRef(null);

  const refreshBeacons = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      const data = await listBeacons();
      setBeacons(data);
      setLastUpdated(new Date());
      setError('');
    } catch (requestError) {
      setError('Beacons could not be loaded. Start the Spring server, then refresh.');
    } finally {
      if (!silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshBeacons();
    const intervalId = window.setInterval(
      () => refreshBeacons({ silent: true }),
      REFRESH_INTERVAL_MS
    );
    return () => window.clearInterval(intervalId);
  }, [refreshBeacons]);

  useEffect(() => {
    function focusSearch(event) {
      const target = event.target;
      const isTyping = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement;
      if ((event.key === '/' && !isTyping) || ((event.metaKey || event.ctrlKey) && event.key === 'k')) {
        event.preventDefault();
        searchRef.current?.focus();
      }
    }
    window.addEventListener('keydown', focusSearch);
    return () => window.removeEventListener('keydown', focusSearch);
  }, []);

  async function generateBeacon() {
    setCreating(true);
    try {
      const created = await createBeacon();
      setBeacons(existingBeacons => [created, ...existingBeacons]);
      setError('');
    } catch (requestError) {
      setError('A beacon could not be generated. Check the Spring server and try again.');
    } finally {
      setCreating(false);
    }
  }

  async function updateBeaconName(id, name) {
    const updated = await renameBeacon(id, name);
    setBeacons(existingBeacons => existingBeacons.map(beacon =>
      beacon.id === id ? updated : beacon
    ));
  }

  async function removeBeacon(id) {
    await deleteBeacon(id);
    setBeacons(existingBeacons => existingBeacons.filter(beacon => beacon.id !== id));
  }

  async function addTestVisit(id) {
    const updated = await recordTestVisit(id);
    setBeacons(existingBeacons => existingBeacons.map(beacon =>
      beacon.id === id ? updated : beacon
    ));
  }

  const normalizedSearch = searchString.trim().toLowerCase();
  const visibleBeacons = beacons.filter(beacon => {
    if (!normalizedSearch) return true;
    return beacon.name.toLowerCase().includes(normalizedSearch)
      || beacon.id.toLowerCase().includes(normalizedSearch)
      || beacon.pixelUrl.toLowerCase().includes(normalizedSearch);
  });

  const summary = useMemo(() => beacons.reduce((totals, beacon) => ({
    opens: totals.opens + beacon.estimatedUniqueOpens,
    loads: totals.loads + beacon.visitCount,
    flagged: totals.flagged + beacon.automatedLoadCount + beacon.mailProxyLoadCount
  }), { opens: 0, loads: 0, flagged: 0 }), [beacons]);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand-lockup">
          <span className="brand-mark" aria-hidden="true">EB</span>
          <div>
            <p className="brand-name">Email Beacon</p>
            <p className="brand-meta">Local tracking console</p>
          </div>
        </div>
        <div className="runtime-state" aria-label="Application status">
          <span className="runtime-dot" aria-hidden="true" />
          <span>Local session</span>
        </div>
      </header>

      <main className="workspace">
        <section className="workspace-heading" aria-labelledby="page-title">
          <div>
            <p className="section-label">Beacon registry</p>
            <h1 id="page-title">Tracking workbench</h1>
          </div>
          <p className="workspace-description">Generate pixels, inspect load confidence, and verify each tracking path from one local console.</p>
        </section>

        <section className="summary-strip" aria-label="Tracking summary">
          <div className="summary-cell"><span className="summary-value">{beacons.length}</span><span className="summary-label">Beacons</span></div>
          <div className="summary-cell"><span className="summary-value">{summary.opens}</span><span className="summary-label">Est. unique opens</span></div>
          <div className="summary-cell"><span className="summary-value">{summary.loads}</span><span className="summary-label">Total loads</span></div>
          <div className="summary-cell"><span className="summary-value">{summary.flagged}</span><span className="summary-label">Proxy + automated</span></div>
        </section>

        <section className="registry" aria-labelledby="registry-title">
          <div className="registry-toolbar">
            <div className="registry-title-group">
              <h2 id="registry-title">Registry</h2>
              <span className="record-count">{visibleBeacons.length} / {beacons.length}</span>
            </div>
            <div className="toolbar-actions">
              <label className="search-field">
                <span className="visually-hidden">Search beacons</span>
                <span className="search-icon" aria-hidden="true">⌕</span>
                <input ref={searchRef} type="search" placeholder="Search name, ID, or URL" value={searchString} onChange={event => setSearchString(event.target.value)} />
                <kbd>⌘ K</kbd>
              </label>
              <button className="button button-secondary" onClick={() => refreshBeacons()} disabled={loading}>{loading && beacons.length > 0 ? 'Refreshing…' : 'Refresh'}</button>
              <button className="button button-primary" onClick={generateBeacon} disabled={creating}>{creating ? 'Generating…' : 'New beacon'}</button>
            </div>
          </div>

          {lastUpdated && <p className="sync-note" aria-live="polite">Synced {lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}<span aria-hidden="true"> · </span>Auto-refreshes every 15 seconds</p>}
          {error && <p className="error-message" role="alert">{error}</p>}
          {loading && beacons.length === 0
            ? <div className="loading-state" role="status">Loading beacon registry…</div>
            : <BeaconsList beacons={visibleBeacons} hasSearch={Boolean(normalizedSearch)} onRename={updateBeaconName} onDelete={removeBeacon} onTest={addTestVisit} onCreate={generateBeacon} />}
        </section>
      </main>

    </div>
  );
}

export default App;
