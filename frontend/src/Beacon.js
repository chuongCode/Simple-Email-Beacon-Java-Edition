import React, { useEffect, useState } from 'react';
import { listVisits } from './api';

export default function Beacon({ data, onRename, onDelete, onTest }) {
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
      setMessage('Test load recorded without changing estimated opens');
    } catch (requestError) {
      setMessage('Could not record test load');
    } finally {
      setBusy(false);
    }
  }

  const embedHtml = `<img src="${data.pixelUrl}" width="1" height="1" alt="">`;

  return (
    <li>
      <div className="beacon-row">
        <div className="beacon-details">
          <div className="beacon-name">
            <input
              type="text"
              placeholder="Add beacon name..."
              value={name}
              maxLength={120}
              disabled={busy}
              onChange={event => setName(event.target.value)}
              onBlur={updateName}
            />
          </div>
          <div className="beacon-id">{data.pixelUrl}</div>
          <div className="beacon-created">Created {new Date(data.createdAt).toLocaleString()}</div>
        </div>
        <div className="status">
          {data.opened
            ? `${data.estimatedUniqueOpens} estimated unique open${data.estimatedUniqueOpens === 1 ? '' : 's'}`
            : 'Not opened'}
          <div className="load-breakdown">
            {data.visitCount} total loads · {data.likelyHumanLoadCount} likely human · {data.mailProxyLoadCount} proxy
            {' · '}{data.automatedLoadCount} automated · {data.duplicateLoadCount} duplicate · {data.testLoadCount} test
          </div>
          {data.firstOpenedAt && (
            <div className="last-opened">First: {new Date(data.firstOpenedAt).toLocaleString()}</div>
          )}
          {data.lastOpenedAt && (
            <div className="last-opened">Last: {new Date(data.lastOpenedAt).toLocaleString()}</div>
          )}
        </div>
        <div className="beacon-actions">
          <button onClick={() => copyText(data.pixelUrl, 'URL copied')}>Copy URL</button>
          <button onClick={() => copyText(embedHtml, 'HTML copied')}>Copy HTML</button>
          <button onClick={addTestVisit} disabled={busy}>Test hit</button>
          <button onClick={toggleVisits} disabled={visitsLoading}>
            {visitsLoading ? 'Loading...' : visits === null ? 'Visits' : 'Hide visits'}
          </button>
          <button onClick={removeBeacon} disabled={busy}>Delete</button>
        </div>
      </div>
      {message && <div className="action-message" aria-live="polite">{message}</div>}
      {visits !== null && (
        <div className="visits">
          {visits.length === 0
            ? <p>No visits recorded yet.</p>
            : (
              <table>
                <thead>
                  <tr><th>Time</th><th>Type</th><th>IP address</th><th>User agent / session data</th></tr>
                </thead>
                <tbody>
                  {visits.map(visit => (
                    <tr key={visit.id}>
                      <td>{new Date(visit.visitedAt).toLocaleString()}</td>
                      <td>
                        {visit.classification}
                        {visit.duplicate && ' · duplicate'}
                        {visit.testVisit && ' · test'}
                      </td>
                      <td>{visit.ipAddress || 'Unknown'}</td>
                      <td>
                        <div>{visit.userAgent || 'Unknown'}</div>
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
