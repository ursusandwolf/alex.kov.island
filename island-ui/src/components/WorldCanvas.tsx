import React, { useEffect, useRef } from 'react';
import { WorldSnapshot } from '../types/simulation';
import { getSpeciesColor } from '../utils/colors';

interface WorldCanvasProps {
  snapshot: WorldSnapshot | null;
  cellSize?: number;
}

const WorldCanvas: React.FC<WorldCanvasProps> = ({ snapshot, cellSize = 12 }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (!snapshot || !canvasRef.current) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const { width, height, nodes } = snapshot;
    canvas.width = width * cellSize;
    canvas.height = height * cellSize;

    // Clear background
    ctx.fillStyle = '#eeeeee';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw nodes
    for (let x = 0; x < width; x++) {
      for (let y = 0; y < height; y++) {
        const node = nodes[x][y];
        const color = getSpeciesColor(node.topSpeciesCode, node.topSpeciesPlant);
        
        ctx.fillStyle = color;
        // Draw cell with a small gap for grid effect
        ctx.fillRect(x * cellSize, y * cellSize, cellSize - 1, cellSize - 1);
      }
    }
  }, [snapshot, cellSize]);

  if (!snapshot) {
    return (
      <div style={{ 
        width: 400, 
        height: 400, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        background: '#fff',
        border: '1px solid #ccc',
        borderRadius: 8
      }}>
        Waiting for simulation data...
      </div>
    );
  }

  return (
    <div style={{ overflow: 'auto', padding: 10, background: '#fff', borderRadius: 8, boxShadow: '0 2px 10px rgba(0,0,0,0.1)' }}>
      <canvas 
        ref={canvasRef} 
        style={{ display: 'block' }}
      />
    </div>
  );
};

export default WorldCanvas;
