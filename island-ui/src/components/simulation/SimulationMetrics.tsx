import React from 'react';

interface SimulationMetricsProps {
  tickCount?: number;
  totalEntityCount?: number;
  width?: number;
  height?: number;
  metrics?: Record<string, string | number>;
}

export const SimulationMetrics: React.FC<SimulationMetricsProps> = ({ tickCount, totalEntityCount, width, height, metrics }) => {
  return (
    <>
      <div style={panelStyle}>
        <h3>Simulation Info</h3>
        <p><strong>Tick:</strong> {tickCount || 0}</p>
        <p><strong>Entities:</strong> {totalEntityCount || 0}</p>
        <p><strong>Dimensions:</strong> {width !== undefined && height !== undefined ? `${width}x${height}` : 'N/A'}</p>
      </div>

      <div style={panelStyle}>
        <h3>Metrics</h3>
        {metrics ? Object.entries(metrics).map(([key, value]) => (
          <div key={key} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px' }}>
            <span style={{ color: '#666' }}>{key}:</span>
            <span style={{ fontWeight: 'bold' }}>{typeof value === 'number' ? value.toLocaleString() : value}</span>
          </div>
        )) : <p>No metrics available</p>}
      </div>
    </>
  );
};

const panelStyle: React.CSSProperties = {
  background: 'white',
  padding: '15px',
  borderRadius: '8px',
  marginBottom: '20px',
  boxShadow: '0 2px 5px rgba(0,0,0,0.05)',
};
