import { test, expect, vi, beforeEach } from 'vitest';
import { useSimulationStore } from './useSimulationStore';

// Mock fetch globally
global.fetch = vi.fn();

beforeEach(() => {
  vi.resetAllMocks();
  // Reset store to initial state
  useSimulationStore.setState({ status: 'IDLE', connected: false, error: null, history: [], snapshot: null });
});

test('updateStatus success updates the status', async () => {
  (global.fetch as any).mockResolvedValueOnce({
    ok: true,
    json: async () => ({ status: 'RUNNING' }),
  });

  await useSimulationStore.getState().updateStatus();

  expect(global.fetch).toHaveBeenCalledWith('/api/v1/simulation/status');
  expect(useSimulationStore.getState().status).toBe('RUNNING');
  expect(useSimulationStore.getState().error).toBeNull();
});

test('pause success updates status', async () => {
  // First mock the pause call
  (global.fetch as any).mockResolvedValueOnce({
    ok: true,
    statusText: 'OK'
  });
  
  // Then mock the subsequent updateStatus call
  (global.fetch as any).mockResolvedValueOnce({
    ok: true,
    json: async () => ({ status: 'PAUSED' }),
  });

  await useSimulationStore.getState().pause();

  expect(global.fetch).toHaveBeenCalledWith('/api/v1/simulation/pause', { method: 'POST' });
  expect(useSimulationStore.getState().status).toBe('PAUSED');
  expect(useSimulationStore.getState().error).toBeNull();
});
