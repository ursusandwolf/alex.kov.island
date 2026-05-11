import { create } from 'zustand';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { WorldSnapshot, SimulationStatus } from '../types/simulation';

interface SimulationState {
  status: SimulationStatus;
  snapshot: WorldSnapshot | null;
  connected: boolean;
  error: string | null;

  // Actions
  connect: () => void;
  disconnect: () => void;
  start: (type: 'nature' | 'simcity') => Promise<void>;
  pause: () => Promise<void>;
  resume: () => Promise<void>;
  stop: () => Promise<void>;
  updateStatus: () => Promise<void>;
}

let stompClient: Client | null = null;

export const useSimulationStore = create<SimulationState>((set, get) => ({
  status: 'IDLE',
  snapshot: null,
  connected: false,
  error: null,

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
      stompClient?.subscribe('/topic/snapshot', (message) => {
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
      const status = await response.text() as SimulationStatus;
      set({ status });
    } catch (err) {
      console.error('Failed to fetch status', err);
    }
  },

  start: async (type) => {
    await fetch(`/api/v1/simulation/start?type=${type}`, { method: 'POST' });
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
}));
