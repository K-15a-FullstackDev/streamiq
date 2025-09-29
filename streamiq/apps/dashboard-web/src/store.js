import { create } from "zustand";
export const useStore = create((set) => ({
  stats: { total: 0, last15m: 0, critical15m: 0 },
  alerts: [],
  addAlert: (a) => set((s) => ({ alerts: [a, ...s.alerts].slice(0, 200) })),
  setStats: (stats) => set({ stats }),
  setAlerts: (arr) => set({ alerts: arr }),
}));
