export const getSpeciesColor = (code: string | null, isPlant: boolean): string => {
  if (!code) return '#ffffff';
  
  // Nature domain colors
  if (isPlant) return '#4caf50'; // Green
  
  const normalized = code.toLowerCase();
  
  // Heuristic for animals
  if (normalized === 'wolf' || normalized === 'bear' || normalized === 'fox') return '#f44336'; // Red - predators
  if (normalized === 'rabbit' || normalized === 'deer' || normalized === 'caterpillar') return '#2196f3'; // Blue - herbivores
  
  // SimCity domain colors
  const cityColors: Record<string, string> = {
    road: '#78909c',
    residential: '#4caf50',
    commercial: '#2196f3',
    industrial: '#ffeb3b'
  };
  
  return cityColors[normalized] ?? '#9c27b0'; // Purple for others
};
