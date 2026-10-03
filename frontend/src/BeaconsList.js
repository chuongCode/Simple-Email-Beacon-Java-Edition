import React from 'react';
import Beacon from './Beacon';

export default function BeaconsList({ beacons, hasSearch, onRename, onDelete, onTest, onCreate }) {
  if (beacons.length === 0) {
    return (
      <div className="empty-state">
        <span className="empty-glyph" aria-hidden="true">□</span>
        <h3>{hasSearch ? 'No matching beacons' : 'No beacons yet'}</h3>
        <p>{hasSearch ? 'Change the search term to inspect another record.' : 'Create a beacon to generate your first tracking URL.'}</p>
        {!hasSearch && <button className="button button-primary" onClick={onCreate}>New beacon</button>}
      </div>
    );
  }

  return (
    <ul className="beacon-list">
      {
        beacons.map((beacon, index) => {
          return <Beacon
            key={beacon.id}
            data={beacon}
            position={index}
            onRename={onRename}
            onDelete={onDelete}
            onTest={onTest}
          />
        })
      }
    </ul>
  );
}
