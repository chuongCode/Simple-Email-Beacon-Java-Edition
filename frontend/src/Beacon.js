import React, { useEffect, useState } from 'react';
import { listVisits } from './api';

const classificationLabels = {
  HUMAN_LIKELY: 'Likely human',
  MAIL_PROXY: 'Mail proxy',
  AUTOMATED: 'Automated',
  TEST: 'Test',
  UNKNOWN: 'Unknown'
};

export default function Beacon({ data, position, onRename, onDelete, onTest }) {
  const [name, setName] = useState(data.name);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [visits, setVisits] = useState(null);
  const [visitsLoading, setVisitsLoading] = useState(false);

  useEffect(() => {
    setName(data.name);
    if (visits !== null && visits.length !== data.visitCount) {
      setVisits(null);
    }
  }, [data.name, data.visitCount, visits]);

  async function updateName() {
    if (name === data.name) return;
    setBusy(true);
    try {
      await onRename(data.id, name);
      setMessage('Name saved');
    } catch (requestError) {
      setName(data.name);
      setMessage('Could not save name');
    } finally {
      setBusy(false);
    }
  }

  async function removeBeacon() {
    const label = data.name || data.id;
    if (!window.confirm(`Delete ${label} and all of its visit records?`)) return;
    setBusy(true);
    try {
      await onDelete(data.id);
    } catch (requestError) {
      setMessage('Could not delete beacon');
      setBusy(false);
    }
  }

  async function copyText(value, successMessage) {
    try {
      await navigator.clipboard.writeText(value);
      setMessage(successMessage);
    } catch (clipboardError) {
      setMessage('Could not copy automatically');
    }
  }

  async function toggleVisits() {
    if (visits !== null) {
      setVisits(null);
      return;
    }
    setVisitsLoading(true);
    try {
      setVisits(await listVisits(data.id));
      setMessage('');
    } catch (requestError) {
      setMessage('Could not load visits');
    } finally {
      setVisitsLoading(false);
    }
  }

  async function addTestVisit() {
    setBusy(true);
    try {
      await onTest(data.id);
      setVisits(null);
      setMessage('Test load recorded. Estimated opens unchanged.');
    } catch (requestError) {
      setMessage('Could not record test load');
    } finally {
      setBusy(false);
    }
  }

  const embedHtml = `<img src="${data.pixelUrl}" width="1" height="1" alt="">`;

  return (
    <li className="beacon-record" style={{ '--record-index': Math.min(position, 5) }}>
      <div className="record-main">
        <div className="record-identity">
          <div className="beacon-name">
            <label className="visually-hidden" htmlFor={`beacon-name-${data.id}`}>Beacon name</label>
            <input
              id={`beacon-name-${data.id}`}
              type="text"
              placeholder="Untitled beacon"
              value={name}
              maxLength={120}
              disabled={busy}
              onChange={event => setName(event.target.value)}
              onBlur={updateName}
            />
          </div>
          <code className="beacon-id">{data.id}</code>
          <p className="beacon-created">Created {new Date(data.createdAt).toLocaleString()}</p>
        </div>

        <div className="record-signal">
          <div className={`open-state ${data.opened ? 'is-opened' : ''}`}>
            <span className="state-dot" aria-hidden="true" />
            {data.opened ? 'Activity detected' : 'Waiting for load'}
          </div>
          <div className="record-metrics">
            <div><strong>{data.estimatedUniqueOpens}</strong><span>Est. opens</span></div>
            <div><strong>{data.visitCount}</strong><span>Total loads</span></div>
            <div><strong>{data.duplicateLoadCount}</strong><span>Duplicates</span></div>
          </div>
          <p className="confidence-line">{data.likelyHumanLoadCount} likely human · {data.mailProxyLoadCount} proxy · {data.automatedLoadCount} automated · {data.testLoadCount} test</p>
          {data.lastOpenedAt && <p className="last-opened">Last signal {new Date(data.lastOpenedAt).toLocaleString()}</p>}
        </div>

        <div className="beacon-actions" aria-label={`Actions for ${data.name || data.id}`}>
          <button className="button button-secondary" onClick={() => copyText(data.pixelUrl, 'URL copied')}>Copy URL</button>
          <button className="button button-secondary" onClick={() => copyText(embedHtml, 'HTML copied')}>Copy HTML</button>
          <button className="button button-secondary" onClick={addTestVisit} disabled={busy}>Test load</button>
          <button className="button button-secondary" onClick={toggleVisits} disabled={visitsLoading} aria-expanded={visits !== null}>
            {visitsLoading ? 'Loading…' : visits === null ? 'Inspect visits' : 'Close visits'}
          </button>
          <button className="button button-danger" onClick={removeBeacon} disabled={busy}>Delete</button>
        </div>
      </div>
      <div className="record-foot">
        <code className="pixel-url">{data.pixelUrl}</code>
        {message && <span className="action-message" aria-live="polite">{message}</span>}
      </div>
      {visits !== null && (
        <div className="visits">
          {visits.length === 0
            ? <p className="visit-empty">No visits recorded for this beacon.</p>
            : (
              <table>
                <caption className="visually-hidden">Recorded visits for {data.name || data.id}</caption>
                <thead>
                  <tr><th>Time</th><th>Type</th><th>IP address</th><th>User agent / session data</th></tr>
                </thead>
                <tbody>
                  {visits.map(visit => (
                    <tr key={visit.id}>
                      <td data-label="Time">{new Date(visit.visitedAt).toLocaleString()}</td>
                      <td data-label="Type">
                        <span className={`visit-type type-${visit.classification.toLowerCase()}`}>{classificationLabels[visit.classification] || visit.classification}</span>
                        {visit.duplicate && <span className="visit-flag">Duplicate</span>}
                      </td>
                      <td data-label="IP address"><code>{visit.ipAddress || 'Unknown'}</code></td>
                      <td data-label="Client data">
                        <div className="user-agent">{visit.userAgent || 'Unknown user agent'}</div>
                        <code>{visit.sessionData}</code>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
        </div>
      )}
    </li>
  )
}
