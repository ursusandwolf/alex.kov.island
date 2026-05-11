export interface NodeSnapshot {
  coordinates: string;
  topSpeciesCode: string | null;
  topSpeciesPlant: boolean;
  hasOrganisms: boolean;
}

export interface WorldSnapshot {
  tickCount: number;
  width: number;
  height: number;
  totalEntityCount: number;
  metrics: Record<string, number>;
  nodes: NodeSnapshot[][];
}

export type SimulationStatus = 'IDLE' | 'RUNNING' | 'PAUSED';
