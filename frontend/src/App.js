import React, { useCallback, useEffect, useState } from 'react';
import BeaconsList from './BeaconsList';
import {
  createBeacon,
  deleteBeacon,
  listBeacons,
  recordTestVisit,
  renameBeacon
} from './api';
import './App.css'

const REFRESH_INTERVAL_MS = 15000;

function App() {
  const [beacons, setBeacons] = useState([]);
  const [searchString, setSearchString] = useState('');
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const [lastUpdated, setLastUpdated] = useState(null);

  const refreshBeacons = useCallback(async ({ silent = false } = {}) => {
    if (!silent) setLoading(true);
    try {
      const data = await listBeacons();
      setBeacons(data);
      setLastUpdated(new Date());
      setError('');
    } catch (requestError) {
      setError('Could not load beacons. Check that the Spring server is running.');
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

  async function generateBeacon() {
    setCreating(true);
    try {
      const created = await createBeacon();
      setBeacons(existingBeacons => [created, ...existingBeacons]);
      setError('');
    } catch (requestError) {
      setError('Could not generate a beacon.');
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

  return (
    <div className="container">
      <div className="controls">
        <input
          type="search"
          placeholder="Search beacons"
          value={searchString}
          onChange={event => setSearchString(event.target.value)}
        />
        <button onClick={() => refreshBeacons()} disabled={loading}>Refresh status</button>
        <button className="generate-button" onClick={generateBeacon} disabled={creating}>
          {creating ? 'Generating...' : '+ Generate beacon'}
        </button>
      </div>
      {lastUpdated && <p className="last-updated">Last updated {lastUpdated.toLocaleTimeString()}</p>}
      {error && <p className="error-message" role="alert">{error}</p>}
      {loading && beacons.length === 0
        ? <p>Loading beacons...</p>
        : <BeaconsList
            beacons={visibleBeacons}
            onRename={updateBeaconName}
            onDelete={removeBeacon}
            onTest={addTestVisit}
          />}
    </div>
  );
}

export default App;
