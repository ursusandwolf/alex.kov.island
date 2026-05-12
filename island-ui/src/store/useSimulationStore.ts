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
    if (stompClient?.active) return;

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
      const data = await response.json();
      set({ status: data.status });
    } catch (err) {
      console.error('Failed to fetch status', err);
    }
  },

  start: async (type, width = 20, height = 20, tickMs = 100) => {
    await fetch(`/api/v1/simulation/start?type=${type}&width=${width}&height=${height}&tickMs=${tickMs}`, { method: 'POST' });
    await get().updateStatus();
  },

  startFromSnapshot: async (filename, type, tickMs = 100) => {
    await fetch(`/api/v1/simulation/start-from-snapshot?filename=${filename}&type=${type}&tickMs=${tickMs}`, { method: 'POST' });
    await get().updateStatus();
  },

  pause: async () => {
    await fetch('/api/v1/simulation/pause', { method: 'POST' });
    await get().updateStatus();
  },

  resume: async () => {
    await fetch('/api/v1/simulation/resume', { method: 'POST' });
    await get().updateStatus();
  },

  stop: async () => {
    await fetch('/api/v1/simulation/stop', { method: 'POST' });
    await get().updateStatus();
    set({ snapshot: null });
  },

  fetchHistory: async () => {
    try {
      const res = await fetch('/api/v1/simulation/snapshot/history');
      if (res.ok) {
        const history = await res.json();
        set({ history });
      }
    } catch (err) {
      console.error('Failed to fetch history', err);
    }
  },

  saveSnapshot: async () => {
    try {
      await fetch('/api/v1/simulation/snapshot/save', { method: 'POST' });
      await get().fetchHistory();
    } catch (err) {
      console.error('Failed to save snapshot', err);
    }
  },

  loadHistoricalSnapshot: async (filename) => {
    try {
      const res = await fetch(`/api/v1/simulation/snapshot/history/${filename}`);
      if (res.ok) {
        const snapshot = await res.json();
        set({ snapshot });
      }
    } catch (err) {
      console.error('Failed to load historical snapshot', err);
    }
  }
}));
