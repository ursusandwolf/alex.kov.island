import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import WorldCanvas from './WorldCanvas';
import { expect, test, vi } from 'vitest';

test('renders waiting message when snapshot is null', () => {
  render(<WorldCanvas snapshot={null} />);
  expect(screen.getByText(/Waiting for simulation data/i)).toBeInTheDocument();
});

test('handles cell click', () => {
  const mockSnapshot = {
    width: 2,
    height: 2,
    tickCount: 1,
    totalEntityCount: 0,
    metrics: {},
    nodes: [
      [
        { coordinates: '0,0', topSpeciesCode: null, topSpeciesPlant: false, hasOrganisms: false, entityCounts: {} },
        { coordinates: '0,1', topSpeciesCode: null, topSpeciesPlant: false, hasOrganisms: false, entityCounts: {} }
      ],
      [
        { coordinates: '1,0', topSpeciesCode: null, topSpeciesPlant: false, hasOrganisms: false, entityCounts: {} },
        { coordinates: '1,1', topSpeciesCode: null, topSpeciesPlant: false, hasOrganisms: false, entityCounts: {} }
      ]
    ]
  };

  const onCellClick = vi.fn();
  const { container } = render(
    <WorldCanvas snapshot={mockSnapshot as any} cellSize={10} onCellClick={onCellClick} />
  );

  const canvas = container.querySelector('canvas');
  expect(canvas).toBeInTheDocument();

  // Mock getBoundingClientRect
  if (canvas) {
    canvas.getBoundingClientRect = vi.fn(() => ({
      left: 0,
      top: 0,
      right: 20,
      bottom: 20,
      width: 20,
      height: 20,
      x: 0,
      y: 0,
      toJSON: () => {}
    }));
    
    // Simulate click on cell (1, 1) i.e. x=15, y=15
    fireEvent.click(canvas, { clientX: 15, clientY: 15 });
  }

  expect(onCellClick).toHaveBeenCalledWith('1,1');
});
