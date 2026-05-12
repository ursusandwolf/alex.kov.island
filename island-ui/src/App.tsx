import React, { useEffect, useState } from 'react';
import { useSimulationStore } from './store/useSimulationStore';
import WorldCanvas from './components/WorldCanvas';
import { NodeSnapshot } from './types/simulation';

function App() {
  const { 
    status, 
    snapshot, 
    connected, 
    connect, 
    disconnect, 
    start, 
    pause, 
    resume, 
    stop, 
    updateStatus 
  } = useSimulationStore();

  const [selectedCoords, setSelectedCoords] = useState<string | null>(null);
  const [configWidth, setConfigWidth] = useState(20);
  const [configHeight, setConfigHeight] = useState(20);
  const [configTickMs, setConfigTickMs] = useState(100);

  useEffect(() => {
    connect();
    updateStatus();
    return () => disconnect();
  }, [connect, disconnect, updateStatus]);

  let selectedNode: NodeSnapshot | null = null;
  if (snapshot && selectedCoords) {
    for (const col of snapshot.nodes) {
      for (const node of col) {
        if (node.coordinates === selectedCoords) {
          selectedNode = node;
          break;
        }
      }
      if (selectedNode) break;
    }
  }

  return (
    <div style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
      <header style={{ marginBottom: '30px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Island Simulator</h1>
        <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
          <span style={{ 
            padding: '5px 12px', 
            borderRadius: '20px', 
            background: connected ? '#e8f5e9' : '#ffebee',
            color: connected ? '#2e7d32' : '#c62828',
            fontSize: '0.9rem',
            fontWeight: 'bold'
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
          <div style={{ marginBottom: '20px', display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', gap: '10px', marginRight: '10px', background: '#f5f5f5', padding: '10px', borderRadius: '8px' }}>
              <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Width: <input type="number" min="1" max="100" value={configWidth} onChange={e => setConfigWidth(Number(e.target.value))} style={inputStyle} /></label>
              <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Height: <input type="number" min="1" max="100" value={configHeight} onChange={e => setConfigHeight(Number(e.target.value))} style={inputStyle} /></label>
              <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Tick (ms): <input type="number" min="10" max="5000" value={configTickMs} onChange={e => setConfigTickMs(Number(e.target.value))} style={inputStyle} /></label>
            </div>
            <button 
              onClick={() => start('nature', configWidth, configHeight, configTickMs)} 
              style={{ ...buttonStyle, background: '#4caf50', color: 'white' }}
            >
              Start Nature
            </button>
            <button 
              onClick={() => start('simcity', configWidth, configHeight, configTickMs)} 
              style={{ ...buttonStyle, background: '#2196f3', color: 'white' }}
            >
              Start SimCity
            </button>
            <button 
              onClick={pause} 
              disabled={status !== 'RUNNING'}
              style={buttonStyle}
            >
              Pause
            </button>
            <button 
              onClick={resume} 
              disabled={status !== 'PAUSED'}
              style={buttonStyle}
            >
              Resume
            </button>
            <button 
              onClick={stop} 
              disabled={status === 'IDLE'}
              style={{ ...buttonStyle, background: '#f44336', color: 'white' }}
            >
              Stop
            </button>
          </div>

          <WorldCanvas 
            snapshot={snapshot} 
            selectedCoords={selectedCoords} 
            onCellClick={setSelectedCoords} 
          />
        </section>

        <aside>
          <div style={panelStyle}>
            <h3>Simulation Info</h3>
            <p><strong>Tick:</strong> {snapshot?.tickCount || 0}</p>
            <p><strong>Entities:</strong> {snapshot?.totalEntityCount || 0}</p>
            <p><strong>Dimensions:</strong> {snapshot ? `${snapshot.width}x${snapshot.height}` : 'N/A'}</p>
          </div>

          <div style={panelStyle}>
            <h3>Metrics</h3>
            {snapshot?.metrics ? Object.entries(snapshot.metrics).map(([key, value]) => (
              <div key={key} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
                <span style={{ color: '#666' }}>{key}:</span>
                <span style={{ fontWeight: 'bold' }}>{typeof value === 'number' ? value.toLocaleString() : value}</span>
              </div>
            )) : <p>No metrics available</p>}
          </div>

          <div style={panelStyle}>
            <h3>Legend</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <LegendItem color="#4caf50" label="Plants / Residential" />
              <LegendItem color="#2196f3" label="Herbivores / Commercial" />
              <LegendItem color="#f44336" label="Predators / Industrial" />
              <LegendItem color="#9c27b0" label="Special / Others" />
            </div>
          </div>

          {selectedNode && (
            <div style={{ ...panelStyle, background: '#e3f2fd', border: '1px solid #90caf9' }}>
              <h3>Cell Details</h3>
              <p><strong>Coordinates:</strong> {selectedNode.coordinates}</p>
              <p><strong>Top Species:</strong> {selectedNode.topSpeciesCode || 'None'}</p>
              <p><strong>Is Plant:</strong> {selectedNode.topSpeciesPlant ? 'Yes' : 'No'}</p>
              <p><strong>Has Organisms:</strong> {selectedNode.hasOrganisms ? 'Yes' : 'No'}</p>
            </div>
          )}
        </aside>
      </main>
    </div>
  );
}

const buttonStyle: React.CSSProperties = {
  padding: '10px 20px',
  border: 'none',
  borderRadius: '4px',
  cursor: 'pointer',
  fontWeight: 'bold',
  boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
};

const inputStyle: React.CSSProperties = {
  width: '60px',
  marginLeft: '5px',
  padding: '4px',
  border: '1px solid #ccc',
  borderRadius: '4px'
};

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

