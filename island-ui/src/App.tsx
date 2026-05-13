import React, { useEffect, useState } from 'react';
import { useSimulationStore } from './store/useSimulationStore';
import WorldCanvas from './components/WorldCanvas';
import { NodeSnapshot } from './types/simulation';
import { SimulationControls } from './components/simulation/SimulationControls';
import { SimulationMetrics } from './components/simulation/SimulationMetrics';
import { SnapshotHistoryPanel } from './components/simulation/SnapshotHistoryPanel';

function App() {
  const { status, snapshot, connected, connect, disconnect, updateStatus, fetchHistory } = useSimulationStore();
  const [selectedCoords, setSelectedCoords] = useState<string | null>(null);
  const [config, setConfig] = useState({ width: 20, height: 20, tickMs: 100 });

  useEffect(() => {
    connect();
    updateStatus();
    fetchHistory();
    return () => disconnect();
  }, [connect, disconnect, updateStatus, fetchHistory]);

  const selectedNode = snapshot?.nodes.flat().find(n => n.coordinates === selectedCoords) || null;

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Island Simulator</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <span style={{ 
            padding: '5px 12px', borderRadius: '20px', background: connected ? '#e8f5e9' : '#ffebee',
            color: connected ? '#2196f3' : '#c62828', fontSize: '0.9rem', fontWeight: 'bold'
          }}>
            {connected ? '● WebSocket Connected' : '○ Disconnected'}
          </span>
          <span style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>
            Status: <span style={{ color: '#1976d2' }}>{status}</span>
          </span>
        </div>
      </header>

      <main style={{ display: 'grid', gridTemplateColumns: '1fr 300px', gap: '30px' }}>
        <section>
          <SimulationControls 
            configWidth={config.width}
            configHeight={config.height}
            configTickMs={config.tickMs}
            onConfigChange={(w, h, t) => setConfig({ width: w, height: h, tickMs: t })}
          />
          <WorldCanvas snapshot={snapshot} selectedCoords={selectedCoords} onCellClick={setSelectedCoords} />
        </section>

        <aside>
          <SimulationMetrics 
            tickCount={snapshot?.tickCount}
            totalEntityCount={snapshot?.totalEntityCount}
            width={snapshot?.width}
            height={snapshot?.height}
            metrics={snapshot?.metrics}
          />

          {selectedNode && (
            <div style={{ ...panelStyle, background: '#e3f2fd', border: '1px solid #90caf9' }}>
              <h3>Cell Details</h3>
              <p><strong>Coordinates:</strong> {selectedNode.coordinates}</p>
              <p><strong>Top Species:</strong> {selectedNode.topSpeciesCode || 'None'}</p>
              <p><strong>Is Plant:</strong> {selectedNode.topSpeciesPlant ? 'Yes' : 'No'}</p>
              <p><strong>Has Organisms:</strong> {selectedNode.hasOrganisms ? 'Yes' : 'No'}</p>
            </div>
          )}

          <SnapshotHistoryPanel configTickMs={config.tickMs} />

          <div style={panelStyle}>
            <h3>Legend</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <LegendItem color="#4caf50" label="Plants / Residential" />
              <LegendItem color="#2196f3" label="Herbivores / Commercial" />
              <LegendItem color="#f44336" label="Predators / Industrial" />
              <LegendItem color="#9c27b0" label="Special / Others" />
            </div>
          </div>
        </aside>
      </main>
    </div>
  );
}

const panelStyle: React.CSSProperties = {
  background: 'white',
  padding: '15px',
  borderRadius: '8px',
  marginBottom: '20px',
  boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
};

const LegendItem = ({ color, label }: { color: string, label: string }) => (
  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
    <div style={{ width: '16px', height: '16px', borderRadius: '3px', background: color }} />
    <span style={{ fontSize: '0.9rem' }}>{label}</span>
  </div>
);

export default App;

