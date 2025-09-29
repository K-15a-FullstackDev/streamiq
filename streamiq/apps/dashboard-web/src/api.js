const API = import.meta.env.VITE_API_BASE;
export async function getStats() {
  const r = await fetch(`${API}/stats`);
  return r.json();
}
export async function getAlerts(limit = 50) {
  const r = await fetch(`${API}/alerts?limit=${limit}`);
  return r.json();
}
export function openSSE(onMsg) {
  const es = new EventSource(`${API}/stream`);
  es.onmessage = (e) => onMsg(JSON.parse(e.data));
  es.onerror = () => es.close();
  return () => es.close();
}
