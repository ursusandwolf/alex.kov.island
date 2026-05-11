export const getSpeciesColor = (code: string | null, isPlant: boolean): string => {
  if (!code) return '#ffffff';
  
  // Nature domain colors
  if (isPlant) return '#4caf50'; // Green
  
  // Simple heuristic for animals if we don't have a full map yet
  if (code.includes('Predator') || code === 'Wolf' || code === 'Bear') return '#f44336'; // Red
  if (code.includes('Herbivore') || code === 'Rabbit' || code === 'Deer') return '#2196f3'; // Blue
  
  // SimCity domain colors (placeholder)
  if (code === 'Residential') return '#4caf50';
  if (code === 'Commercial') return '#2196f3';
  if (code === 'Industrial') return '#ffeb3b';
  
  return '#9c27b0'; // Purple for others
};
