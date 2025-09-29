import { useEffect } from "react";
import { getStats, getAlerts, openSSE } from "./api";
import { useStore } from "./store";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
} from "recharts";

export default function App() {
  const { stats, setStats, alerts, setAlerts, addAlert } = useStore();

  useEffect(() => {
    (async () => {
      setStats(await getStats());
      setAlerts(await getAlerts(50));
    })();
    const close = openSSE((msg) =>
      addAlert({
        event_id: msg.event_id,
        ts: msg.ts,
        source: msg.source,
        level: msg.anomaly?.level,
        score: msg.anomaly?.score,
        anomaly: msg.anomaly,
      })
    );
    return close;
  }, []);

  const data = alerts
    .slice(0, 50)
    .map((a) => ({
      t: new Date(a.ts).toLocaleTimeString(),
      score: a.score || a.anomaly?.score || 0,
    }));

  return (
    <div className="p-6 space-y-6">
      <h1>StreamIQ - Live Alerts</h1>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(3,1fr)",
          gap: 12,
        }}
      >
        <Card title="Total Alerts" value={stats.total} />
        <Card title="Last 15 min" value={stats.last15m} />
        <Card title="Critical (15m)" value={stats.critical15m} />
      </div>

      <div style={{ height: 240, border: "1px solid #ddd", padding: 8 }}>
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data}>
            <XAxis dataKey="t" />
            <YAxis />
            <Tooltip />
            <Line type="monotone" dataKey="score" dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <table
        width="100%"
        cellPadding="8"
        style={{ borderCollapse: "collapse" }}
      >
        <thead>
          <tr>
            <th>Time</th>
            <th>Level</th>
            <th>Score</th>
            <th>Source</th>
            <th>Event</th>
          </tr>
        </thead>
        <tbody>
          {alerts.map((a, i) => (
            <tr
              key={a.event_id + ":" + i}
              style={{ borderTop: "1px solid #eee" }}
            >
              <td>{new Date(a.ts).toLocaleString()}</td>
              <td>{a.level || a.anomaly?.level}</td>
              <td>{a.score || a.anomaly?.score}</td>
              <td>{a.source}</td>
              <td style={{ fontFamily: "monospace" }}>{a.event_id}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function Card({ title, value }) {
  return (
    <div style={{ border: "1px solid #ddd", padding: 12, borderRadius: 8 }}>
      <div>{title}</div>
      <div style={{ fontSize: 28, fontWeight: 700 }}>{value}</div>
    </div>
  );
}
