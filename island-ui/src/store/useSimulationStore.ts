import { create } from 'zustand';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WorldSnapshot, SimulationStatus } from '../types/simulation';

interface SimulationState {
  status: SimulationStatus;
  snapshot: WorldSnapshot | null;
  connected: boolean;
  error: string | null;
  history: string[];

  // Actions
  connect: () => void;
  disconnect: () => void;
  start: (type: 'nature' | 'simcity', width?: number, height?: number, tickMs?: number) => Promise<void>;
  startFromSnapshot: (filename: string, type: 'nature' | 'simcity', tickMs?: number) => Promise<void>;
  pause: () => Promise<void>;
  resume: () => Promise<void>;
  stop: () => Promise<void>;
  updateStatus: () => Promise<void>;
  
  // History Actions
  fetchHistory: () => Promise<void>;
  saveSnapshot: () => Promise<void>;
  loadHistoricalSnapshot: (filename: string) => Promise<void>;
}

let stompClient: Client | null = null;

export const useSimulationStore = create<SimulationState>((set, get) => ({
  status: 'IDLE',
  snapshot: null,
  connected: false,
  error: null,
  history: [],

  connect: () => {
    if (stompClient) {
      stompClient.deactivate();
    }

    stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws-simulation'),
      debug: (str) => console.log('STOMP: ' + str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    stompClient.onConnect = (_frame) => {
      set({ connected: true, error: null });
      console.log('Connected to STOMP');
      stompClient?.subscribe('/topic/world-state', (message) => {
        // Only update live if we are running/paused, not just viewing history
        // If status is IDLE, we might be viewing history. Let's still accept updates but maybe flag it.
        const snapshot: WorldSnapshot = JSON.parse(message.body);
        set({ snapshot });
      });
    };

    stompClient.onStompError = (frame) => {
      set({ error: 'STOMP error: ' + frame.headers['message'] });
    };

    stompClient.onWebSocketClose = () => {
      set({ connected: false });
    };

    stompClient.activate();
  },

  disconnect: () => {
    stompClient?.deactivate();
    set({ connected: false, snapshot: null });
  },

  updateStatus: async () => {
    try {
      const response = await fetch('/api/v1/simulation/status');
      if (!response.ok) throw new Error(`Status fetch failed: ${response.statusText}`);
      const data = await response.json();
      set({ status: data.status, error: null });
    } catch (err) {
      set({ error: String(err) });
      console.error('Failed to fetch status', err);
    }
  },

  start: async (type, width = 20, height = 20, tickMs = 100) => {
    try {
      const res = await fetch(`/api/v1/simulation/start?type=${type}&width=${width}&height=${height}&tickMs=${tickMs}`, { method: 'POST' });
      if (!res.ok) throw new Error(`Start failed: ${await res.text()}`);
      await get().updateStatus();
    } catch (err) {
      set({ error: String(err) });
    }
  },

  startFromSnapshot: async (filename, type, tickMs = 100) => {
    try {
      const res = await fetch(`/api/v1/simulation/start-from-snapshot?filename=${filename}&type=${type}&tickMs=${tickMs}`, { method: 'POST' });
      if (!res.ok) throw new Error(`Start from snapshot failed: ${await res.text()}`);
      await get().updateStatus();
    } catch (err) {
      set({ error: String(err) });
    }
  },

  pause: async () => {
    try {
      const res = await fetch('/api/v1/simulation/pause', { method: 'POST' });
      if (!res.ok) throw new Error(`Pause failed: ${res.statusText}`);
      await get().updateStatus();
    } catch (err) {
      set({ error: String(err) });
    }
  },

  resume: async () => {
    try {
      const res = await fetch('/api/v1/simulation/resume', { method: 'POST' });
      if (!res.ok) throw new Error(`Resume failed: ${res.statusText}`);
      await get().updateStatus();
    } catch (err) {
      set({ error: String(err) });
    }
  },

  stop: async () => {
    try {
      const res = await fetch('/api/v1/simulation/stop', { method: 'POST' });
      if (!res.ok) throw new Error(`Stop failed: ${res.statusText}`);
      await get().updateStatus();
      set({ snapshot: null });
    } catch (err) {
      set({ error: String(err) });
    }
  },

  fetchHistory: async () => {
    try {
      const res = await fetch('/api/v1/simulation/snapshot/history');
      if (res.ok) {
        const history = await res.json();
        set({ history, error: null });
      } else {
        throw new Error(`History fetch failed: ${res.statusText}`);
      }
    } catch (err) {
      set({ error: String(err) });
      console.error('Failed to fetch history', err);
    }
  },

  saveSnapshot: async () => {
    try {
      const res = await fetch('/api/v1/simulation/snapshot/save', { method: 'POST' });
      if (!res.ok) throw new Error(`Save failed: ${await res.text()}`);
      await get().fetchHistory();
    } catch (err) {
      set({ error: String(err) });
    }
  },

  loadHistoricalSnapshot: async (filename) => {
    try {
      const res = await fetch(`/api/v1/simulation/snapshot/history/${filename}`);
      if (res.ok) {
        const snapshot = await res.json();
        set({ snapshot, error: null });
      } else {
        throw new Error(`Historical snapshot load failed: ${res.statusText}`);
      }
    } catch (err) {
      set({ error: String(err) });
      console.error('Failed to load historical snapshot', err);
    }
  }
}));
