import React from 'react';
import Beacon from './Beacon';

export default function BeaconsList({ beacons, onRename, onDelete, onTest }) {
  if (beacons.length === 0) {
    return <p>No beacons found. Generate one to get started.</p>;
  }

  return (
    <ul>
      {
        beacons.map(beacon => {
          return <Beacon
            key={beacon.id}
            data={beacon}
            onRename={onRename}
            onDelete={onDelete}
            onTest={onTest}
          />
        })
      }
    </ul>
  );
}
