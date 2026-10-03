import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
});

export async function listBeacons() {
  const response = await api.get('/beacons');
  return response.data;
}

export async function createBeacon(name = '') {
  const response = await api.post('/beacons', { name });
  return response.data;
}

export async function renameBeacon(id, name) {
  const response = await api.patch(`/beacons/${id}`, { name });
  return response.data;
}

export async function deleteBeacon(id) {
  await api.delete(`/beacons/${id}`);
}

export async function listVisits(id) {
  const response = await api.get(`/beacons/${id}/visits`);
  return response.data;
}

