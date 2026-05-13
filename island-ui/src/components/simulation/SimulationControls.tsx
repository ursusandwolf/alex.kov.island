import React from 'react';
import { useSimulationStore } from '../../store/useSimulationStore';

interface SimulationControlsProps {
  configWidth: number;
  configHeight: number;
  configTickMs: number;
  onConfigChange: (width: number, height: number, tickMs: number) => void;
}

export const SimulationControls: React.FC<SimulationControlsProps> = ({ configWidth, configHeight, configTickMs, onConfigChange }) => {
  const { status, start, pause, resume, stop, saveSnapshot } = useSimulationStore();

  return (
    <div style={{ marginBottom: '20px', display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
      <div style={{ display: 'flex', gap: '10px', marginRight: '10px', background: '#f5f5f5', padding: '10px', borderRadius: '8px' }}>
        <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Width: <input type="number" min="1" max="100" value={configWidth} onChange={e => onConfigChange(Number(e.target.value), configHeight, configTickMs)} style={inputStyle} /></label>
        <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Height: <input type="number" min="1" max="100" value={configHeight} onChange={e => onConfigChange(configWidth, Number(e.target.value), configTickMs)} style={inputStyle} /></label>
        <label style={{ fontSize: '0.9rem', fontWeight: 'bold' }}>Tick (ms): <input type="number" min="10" max="5000" value={configTickMs} onChange={e => onConfigChange(configWidth, configHeight, Number(e.target.value))} style={inputStyle} /></label>
      </div>
      <button onClick={() => start('nature', configWidth, configHeight, configTickMs)} style={{ ...buttonStyle, background: '#4caf50', color: 'white' }}>Start Nature</button>
      <button onClick={() => start('simcity', configWidth, configHeight, configTickMs)} style={{ ...buttonStyle, background: '#2196f3', color: 'white' }}>Start SimCity</button>
      <button onClick={pause} disabled={status !== 'RUNNING'} style={buttonStyle}>Pause</button>
      <button onClick={resume} disabled={status !== 'PAUSED'} style={buttonStyle}>Resume</button>
      <button onClick={stop} disabled={status === 'IDLE'} style={{ ...buttonStyle, background: '#f44336', color: 'white' }}>Stop</button>
      <button onClick={saveSnapshot} disabled={status === 'IDLE'} style={{ ...buttonStyle, background: '#ff9800', color: 'white', marginLeft: 'auto' }}>Save Snapshot</button>
    </div>
  );
};

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
